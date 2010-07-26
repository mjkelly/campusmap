#!/usr/bin/python
# -----------------------------------------------------------------
# parse.py -- Parse location and path xml data files
# -----------------------------------------------------------------

import os
import optparse
import xml.dom.minidom

class Location(object):
    def __init__(self):
        pass

    def __setattr__(self, name, value):
        valid_attributes = [ 'id', 
            'x', 
            'y', 
            'name', 
            'passThrough', 
            'intersect', 
            'displayName', 
            'aliases',
            'keywords',
            'code' 
        ]

        if not name in valid_attributes:
            raise RuntimeError('Unknown attribute: %s' % name)
        self.__dict__[name] = value

    def __str__(self):
        return '<Location: %s %s>' % (self.id, self.name)

    def __repr__(self):
        return self.__str__()

class Point(object):
    def __init__(self, x, y):
        self.x = int(x)
        self.y = int(y)

class Path(list):
    def append(self, x, y):
        list.append(self, Point(x, y))

def parseLocations(file):
    doc = xml.dom.minidom.parse(file)

    locations = list()

    for node in doc.getElementsByTagName('location'):
        loc = Location()
        # Set attributes by name
        for key in node.attributes.keys():
            value = node.attributes[key].value

            int_keys = [ 'x', 'y', 'id' ]
            bool_keys = [ 'passThrough', 'intersect', 'displayName' ]

            if key in int_keys:
                value = int(value)
            elif key in bool_keys:
                if value == 'true':
                    value = True
                elif value == 'false':
                    value = False
                else:
                    raise RuntimeError('Unknown boolean value for key, %s: %s' % (key, value))
            setattr(loc, key, value)

        # Set text data child of elements by name
        for child in node.childNodes:
            name = child.nodeName
            # Single text child
            if len(child.childNodes) == 1 and child.childNodes[0].__class__ == xml.dom.minidom.Text:
                data = child.firstChild.data
                setattr(loc, name, data)
            # Set of elements containing text children
            elif child.__class__ == xml.dom.minidom.Element:
                arr = []
                for subchild in child.childNodes:
                     if len(subchild.childNodes) == 1 and subchild.childNodes[0].__class__ == xml.dom.minidom.Text:
                        arr.append(subchild.firstChild.data)
                setattr(loc, name, arr)
                           
        locations.append(loc)
    return locations

def parsePaths(file):
    doc = xml.dom.minidom.parse(file)
    
    paths = list()

    for node in doc.getElementsByTagName('path'):
        p = Path()
        for child in node.childNodes:
            if child.__class__ == xml.dom.minidom.Element:
                x = None
                y = None

                for key in child.attributes.keys():
                    if key == 'x':
                        x = child.attributes[key].value
                    if key == 'y':
                        y = child.attributes[key].value

                if x is None or y is None:
                    raise RuntimeError('Error parsing point!')

                p.append(x, y)
        paths.append(p)
    return paths

def main():
    parser = optparse.OptionParser()
    default_data = 'data'
    parser.add_option('--data', help='Path to data folder containing xml files.  [ default = %s ]' % default_data, default=default_data)
    default_locations = 'optimizedLocations.xml'
    parser.add_option('--locations', help='Name of xml file containing location data [ default = %s ]' % default_locations, default=default_locations)
    default_path = 'optimizedPath.xml'
    parser.add_option('--paths', help='Name of xml file containing path data [ default = %s ]' % default_path, default=default_path)
    (opts, args) = parser.parse_args()

    data_dir = opts.data
    locations_file = os.path.join(data_dir, opts.locations)
    paths_file      = os.path.join(data_dir, opts.paths)

    locations = parseLocations(locations_file)
    paths     = parsePaths(paths_file)

    print 'Loaded %d locations.' % len(locations)
    print 'Loaded %d paths.' % len(paths)   

    # This datastructure is much like the one we use in the old map.cgi. It
    # contains multipe references to Location objects (one possibly in each
    # field).
    loc_lookup = {'ByKeyword': {}, 'ByID': {}, 'ByCode': {}}
    for l in locations:
        if l.displayName:
            loc_lookup['ByID'][int(l.id)] = l

            if hasattr(l, 'code'):
                loc_lookup['ByCode'][str(l.code)] = l

            if hasattr(l, 'keywords'):
                for keyword in l.keywords.split(' '):
                    if keyword not in loc_lookup['ByKeyword']:
                        loc_lookup['ByKeyword'][str(keyword)] = []
                    loc_lookup['ByKeyword'][str(keyword)].append(l)

    print loc_lookup

if __name__ == '__main__':
    main()
