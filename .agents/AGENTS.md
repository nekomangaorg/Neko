# Custom Rules

- **Do NOT auto commit anything**: Never automatically run `git commit`. Keep all modifications unstaged in the working tree so that the user can review them and commit them manually.
- **Always generate a commit message**: At the end of every task involving code modifications, always output a draft commit message matching conventional commit standards. The commit message header must not include a scope in parentheses (e.g., use `style: description` instead of `style(scope): description`).
- **Always import classes when possible**: Do not use fully qualified class names in type signatures or code references (e.g. use `viewModel: ReaderViewModel` instead of `viewModel: eu.kanade.tachiyomi.ui.reader.ReaderViewModel` by adding the corresponding `import` statement at the top of the file).

