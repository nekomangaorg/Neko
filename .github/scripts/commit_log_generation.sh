#!/bin/bash

OUTPUT=$(curl -H "Accept: application/vnd.github.v3+json" \
  "https://api.github.com/repos/nekomangaorg/neko/compare/$1...$2" \
  | jq -r ' # The -r flag is essential here!
    # Define a helper to get the normalized message for sorting/filtering AND final output.
    # This applies all the normalization rules.
    def get_normalized_line:
      (.message[0]? // "" | select(type == "string") // "") # Start with safe first line
      # Apply normalization rules using sub (regex substitute) in a single chain
      # Ensure order: specific replaces first, then general replaces, then removals.
      | sub("^fix\\(deps\\)"; "chore(deps)")  # 1. fix(deps) -> chore(deps)
      | sub("^feature/"; "feat:")           # 2. feature/ -> feat:
      | sub("^fix/"; "fix:")                 # 3. fix/ -> fix:
      | sub("^fix "; "fix: ")                 # 4. fix -> fix:
      | sub("^chore/"; "chore:")             # 5. chore/ -> chore:
      | sub("^chore\\(deps\\):"; "chore:");   # 6. chore(deps): -> chore:

    # Assign priority for sorting using the normalized message
    def get_sort_priority:
      get_normalized_line as $line |
      if ($line | startswith("feat")) then 0
      elif ($line | startswith("fix")) then 1
      elif ($line | startswith("opt")) then 2
      elif ($line | startswith("ref")) then 3
      elif ($line | startswith("chore")) then 4
      else 5 # For "anything else"
      end;

    # --- Main processing starts here ---

    # First, transform the raw API output into the desired object structure
    [ .commits[] |
      {
        message: (.commit.message | split("\n")),
        username: .author.login
      }
    ] |

    # Now, partition the array based on the normalized message prefix.
    reduce .[] as $item ({
      prefixed: [],
      other: []
    };
      # Use the defined functions on the current item ($item)
      if ($item | get_sort_priority) < 3 then
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
    # The -r flag on jq will ensure no JSON quotes.
    # We are explicitly constructing the string here.
    .[] | "- \(get_normalized_line) (@\(.username))"
  ')

# Now, remove " (@nonproto)" from the entire multi-line OUTPUT variable.
# Using 'sed' to perform the substitution on the variable's content.
# The 'g' flag ensures global replacement on each line.
# The 'i' flag ensures case-insensitive matching.
CLEANED_OUTPUT=$(echo "$OUTPUT" | sed 's/ (@nonproto)//gi')

# Now, to echo the variable without re-quoting and preserving newlines:
printf "%b\n" "$CLEANED_OUTPUT"
