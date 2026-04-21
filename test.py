import urllib.request
import re

url = 'https://developer.android.com/reference/android/provider/Settings'
html = urllib.request.urlopen(url).read().decode('utf-8')
matches = re.finditer(r'ACTION_APP_OPEN_BY_DEFAULT_SETTINGS.*?API level (\d+)', html, re.DOTALL)
for match in matches:
    print(match.group(1))
