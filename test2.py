import urllib.request
from bs4 import BeautifulSoup
url = 'https://developer.android.com/reference/android/provider/Settings'
html = urllib.request.urlopen(url).read()
soup = BeautifulSoup(html, 'html.parser')
el = soup.find(id='ACTION_APP_OPEN_BY_DEFAULT_SETTINGS')
if el:
    print(el.find_next(class_='api-level').text)
