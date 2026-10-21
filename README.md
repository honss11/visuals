# LikeVisuals - Fabric 1.21.4

Build: push the folder to GitHub (with the hidden .github folder) -> Actions -> build -> LikeVisuals-jar,
or locally with Java 21 + Gradle 8.12: `gradle build`.

Right Shift opens the menu. Drag HUD elements right on the screen (outside the window), scroll to resize,
Tab (or the "Edit HUD" button) shows every HUD element so you can toggle/move them.

Chat commands (prefix "." or "/lv"):
  .bind <module|item> <key>      e.g. .bind autoswap g   .bind trap x   .bind ender pearl c   (use "none" to unbind)
  .bind list | remove <name|key> | clear
  .friend add|remove|list   .mark add|remove|list   .cfg save|load|delete|list <name>   .auto add|remove|list   .hud
