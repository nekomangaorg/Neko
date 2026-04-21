import urllib.request
import re
html=urllib.request.urlopen('https://developer.android.com/reference/android/provider/Settings').read().decode('utf-8')
matches = re.findall(r'ACTION_APP_OPEN_BY_DEFAULT_SETTINGS', html)
print(matches)
