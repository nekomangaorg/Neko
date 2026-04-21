import xml.etree.ElementTree as ET

tree = ET.parse('constants/src/main/res/values/strings.xml')
root = tree.getroot()
for child in root:
    if 'link' in child.attrib.get('name', '').lower() or 'default' in child.attrib.get('name', '').lower() or 'open' in child.attrib.get('name', '').lower():
        print(child.attrib.get('name', ''), child.text)
