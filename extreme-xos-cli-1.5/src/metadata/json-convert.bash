#/bin/bash

sed -i '
s/^\(\s\+\)\"STATE\"/\1\"state\"/g
s/\"STATE\"/\"nextState\"/g
s/\"PROMPT_DEVICE\"/\"promptDevice\"/g
s/\"PROMPT_PRIV\"/\"promptPriv\"/g
s/\"PROMPT_PROXY\"/\"promptProxy\"/g
s/\"PROMPT_MENU\"/\"promptMenu\"/g
s/\"PROMPT_LOGIN\"/\"promptLogin\"/g
s/\"PROMPT_PASSWORD\"/\"promptPassword\"/g
s/\"PROMPT_IGNORE\"/\"promptIgnore\"/g
s/\"ERROR_DEFAULT\"/\"promptError\"/g
s/\"MENU_CMD\"/\"menuCmd\"/g
s/\"\([A-Z0-9]\+\)\"/\"\L\1\"/g
' "$@"
