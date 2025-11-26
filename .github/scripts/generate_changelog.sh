#!/bin/bash

# Check if token is present
if [ -z "$GITHUB_TOKEN" ]; then
  echo "Error: GITHUB_TOKEN is not set." >&2
  exit 1
fi

OUTPUT=$(curl -s -f -H "Authorization: token $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github.com/v3+json" \
  "https://api.github.com/repos/nekomangaorg/neko/compare/$1...$2" \
  | jq -r '
    # --- Normalization and Sorting Helpers ---

    def get_normalized_line:
      (.message[0]? // "" | select(type == "string") // "")
      # 1. Fix double colons: consume the optional colon in the match
      | sub("^fix\\(deps\\):?"; "chore:")
      | sub("^chore\\(deps\\):?"; "chore:")
      | sub("^feature/"; "feat:")
      # 2. Handle "Fix:" (capitalized with colon) explicitly
      | sub("^(fix|Fix):"; "fix:")
      | sub("^(fix|Fix)/"; "fix:")
      | sub("^(fix|Fix)\\s"; "fix: "; "i")
      | sub("^chore/"; "chore:")
      | sub("^opt/"; "opt:")
      | sub("^ref/"; "ref:")
      # 3. Fix corruption: Use named capture group (?<p>...) instead of \1
      | sub("^(?<p>feat|fix|opt|ref|chore):\\s*"; "\(.p): ")
      | sub("^\\s*"; "");

    def get_sort_priority:
      get_normalized_line as $line |
      if ($line | startswith("feat:")) then 0
      elif ($line | startswith("fix:")) then 1
      elif ($line | startswith("opt:")) then 2
      elif ($line | startswith("ref:")) then 3
      elif ($line | startswith("chore:")) then 4
      else 5
      end;

    def is_merge_commit:
      (.message[0]? // "" | select(type == "string") // "") |
      startswith("Merge remote-tracking branch '\''origin/main'\''");

    # --- Main processing starts here ---

    [ .commits[] |
      {
        message: (.commit.message | split("\n")),
        username: .author.login
      }
      | select(is_merge_commit | not)
    ] |

    reduce .[] as $item ({
      prefixed: [],
      other: []
    };
      if ($item | get_sort_priority) < 5 then
        .prefixed += [$item]
      else
        .other += [$item]
      end
    ) |

    (.prefixed | sort_by(get_sort_priority, (. | get_normalized_line | ascii_downcase))) as $sorted_prefixed |
    (.other | sort_by(. | get_normalized_line | ascii_downcase)) as $sorted_other |

    $sorted_prefixed + $sorted_other |

    .[] | "- \(get_normalized_line) (@\(.username))"
  ')

# Check if OUTPUT is empty
if [ -z "$OUTPUT" ]; then
  echo "Error: Output is empty." >&2
  exit 1
fi

# Cleanup step
CLEANED_OUTPUT=$(echo "$OUTPUT" | sed 's/ (@nonproto)//gi')

printf "%b\n" "$CLEANED_OUTPUT"
