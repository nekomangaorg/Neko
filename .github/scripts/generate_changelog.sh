#!/bin/bash

OUTPUT=$(curl -H "Accept: application/vnd.github.com/v3+json" \
  "https://api.github.com/repos/nekomangaorg/neko/compare/$1...$2" \
  | jq -r ' # The -r flag is essential here!

    # --- Normalization and Sorting Helpers ---

    # Define a helper to get the normalized message for sorting/filtering AND final output.
    def get_normalized_line:
      (.message[0]? // "" | select(type == "string") // "") # Start with safe first line
      # Apply normalization rules using sub (regex substitute) in a single chain
      # Use the "i" flag for case-insensitive matching where appropriate (like for Fix/fix)
      # Ensure order: specific replaces first, then general replaces, then removals.
      | sub("^fix\\(deps\\)"; "chore:")               # 1. fix(deps) -> chore:
      | sub("^feature/"; "feat:")                      # 2. feature/ -> feat:
      | sub("^(fix|Fix)/"; "fix:")                     # 3. fix/ OR Fix/ -> fix:
      | sub("^(fix|Fix)\\s"; "fix: "; "i")             # 4. fix OR Fix followed by space -> fix: (Case-insensitive fix handling)
      | sub("^chore/"; "chore:")                      # 5. chore/ -> chore:
      | sub("^chore\\(deps\\):"; "chore:")            # 6. chore(deps): -> chore:
      | sub("^opt/"; "opt:")                          # 7. opt/ -> opt:
      | sub("^ref/"; "ref:")                          # 8. ref/ -> ref:
      | sub("^(feat|fix|opt|ref|chore):\\s*"; "\\1: ") # 9. Ensure a space after prefix:
      | sub("^\\s*"; "")                               # 10. Trim leading whitespace

    # Assign priority for sorting using the normalized message
    # Order: feat:, fix:, opt:, ref:, chore:, anything else
    def get_sort_priority:
      get_normalized_line as $line |
      if ($line | startswith("feat:")) then 0
      elif ($line | startswith("fix:")) then 1
      elif ($line | startswith("opt:")) then 2
      elif ($line | startswith("ref:")) then 3
      elif ($line | startswith("chore:")) then 4
      else 5 # For "anything else"
      end;

    # Define a filter to exclude specific messages
    def is_merge_commit:
      (.message[0]? // "" | select(type == "string") // "") |
      startswith("Merge remote-tracking branch 'origin/main'")
      ;

    # --- Main processing starts here ---

    # First, transform the raw API output into the desired object structure and filter out merges
    [ .commits[] |
      {
        message: (.commit.message | split("\n")),
        username: .author.login
      }
      # Filter: Exclude 'Merge remote-tracking branch 'origin/main''
      | select(is_merge_commit | not)
    ] |

    # Now, partition the array based on the normalized message prefix for sorting.
    # Group by priority 0-4 (feat, fix, opt, ref, chore) and 5 (other)
    reduce .[] as $item ({
      prefixed: [],
      other: []
    };
      # Use the defined functions on the current item ($item)
      if ($item | get_sort_priority) < 5 then
        .prefixed += [$item]
      else
        .other += [$item]
      end
    ) |

    # Sort the prefixed commits by priority and then alphabetically by normalized message
    (.prefixed | sort_by(get_sort_priority, (. | get_normalized_line | ascii_downcase))) as $sorted_prefixed |

    # Sort the other commits alphabetically by normalized message
    (.other | sort_by(. | get_normalized_line | ascii_downcase)) as $sorted_other |

    # Concatenate the two sorted lists
    $sorted_prefixed + $sorted_other |

    # Final formatting for output, using the NORMALIZED message line
    .[] | "- \(get_normalized_line) (@\(.username))"
  ')

# --- MODIFIED CLEANUP STEP ---
# Remove " (@nonproto)" from the entire multi-line OUTPUT variable.
# The previous version already handled this; I've ensured it's robust.
CLEANED_OUTPUT=$(echo "$OUTPUT" | sed 's/ (@nonproto)//gi')

# Now, to echo the variable without re-quoting and preserving newlines:
printf "%b\n" "$CLEANED_OUTPUT"
