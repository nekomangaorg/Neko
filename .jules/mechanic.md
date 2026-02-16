## 2025-05-23 - ViewBinding Obfuscates Unused Resource Search
**Learning:** When searching for unused XML layouts, simple `grep` for `R.layout.layout_name` is insufficient if the project uses ViewBinding. ViewBinding generates classes (e.g., `ReaderTransitionViewBinding` from `reader_transition_view.xml`) that inflate the layout without explicitly referencing the `R.layout` ID in the source code.
**Action:** Always search for the generated Binding class name (e.g., `LayoutNameBinding`) in addition to the resource ID when verifying if a layout is unused.
