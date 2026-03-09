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
      # 1. Standardize common prefixes and handle agent-specific ones (opt, perf, ref)
      | sub("^feat(ure)?/"; "feat:")
      | sub("^(fix|Fix)[:/ ]*"; "fix: ")
      | sub("^opt/"; "perf:")
      | sub("^perf/"; "perf:")
      | sub("^ref/"; "ref:")
      | sub("^(chore|fix)\\(deps\\):?"; "chore: ")
      | sub("^chore/"; "chore:")
      | sub("^style/"; "style:")
      | sub("^test/"; "test:")
      # 2. Cleanup: Ensure a single space after the colon and remove leading whitespace
      | sub("^(?<p>feat|fix|perf|ref|chore|style|test):\\s*"; "\(.p): ")
      | sub("^\\s*"; "");

    def get_sort_priority:
      get_normalized_line as $line |
      if ($line | startswith("feat:")) then 0      # New Capabilities
      elif ($line | startswith("fix:")) then 1     # Bug Fixes / Security
      elif ($line | startswith("perf:")) then 2    # Performance
      elif ($line | startswith("ref:")) then 3     # Structural
      elif ($line | startswith("style:")) then 4   # Formatting
      elif ($line | startswith("test:")) then 5    # Coverage
      elif ($line | startswith("chore:")) then 6   # Maintenance
      else 7
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
