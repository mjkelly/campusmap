#!/usr/bin/python
# -----------------------------------------------------------------
# parse.py -- Parse location and path xml data files
# -----------------------------------------------------------------

# XXX BEWARE XXX
# This reads the XML versions of the locations file. There may be skew between
# this data and the binary-format data!

import os
import optparse
import xml.dom.minidom
import pickle
import pprint

class Location(object):
    valid_attributes = ['id', 
        'x', 
        'y', 
        'name', 
        'passThrough', 
        'intersect', 
        'displayName', 
        'aliases',
        'keywords',
        'code']

    translate_attributes = ['id', 
        'x', 
        'y', 
        'name', 
        'aliases',
        'keywords',
        'code']

    def __init__(self):
        pass

    def __setattr__(self, name, value):

        if not name in self.valid_attributes:
            raise RuntimeError('Unknown attribute: %s' % name)
        self.__dict__[name] = value

    def __str__(self):
        return '<Location: %s %s>' % (self.id, self.name)

    def __repr__(self):
        return self.__str__()

    def to_dict(self):
        d = {} 
        for attr in self.translate_attributes:
        #for attr in self.valid_attributes:
            if hasattr(self, attr):
                d[attr] = getattr(self, attr)
        return d
        

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
    default_output = 'locations.pickle'
    default_locations = 'optimizedLocations.xml'

    parser.add_option('--data', help='Path to data folder containing xml files.  [ default = %s ]' % default_data, default=default_data)
    parser.add_option('--locations', help='Name of xml file containing location data [ default = %s ]' % default_locations, default=default_locations)
    default_path = 'optimizedPath.xml'
    parser.add_option('--paths', help='Name of xml file containing path data [ default = %s ]' % default_path, default=default_path)
    parser.add_option('--output', help='Pickle output file  [ default = %s ]' % default_output, default=default_output)
    (opts, args) = parser.parse_args()

    data_dir = opts.data
    locations_file = os.path.join(data_dir, opts.locations)
    paths_file      = os.path.join(data_dir, opts.paths)

    locations = parseLocations(locations_file)
    paths     = parsePaths(paths_file)

    visible_locs = [l for l in locations if l.displayName]

    print 'Loaded %d locations (%d visible).' % (len(locations), len(visible_locs))
    print 'Loaded %d paths.' % len(paths)   

    # This datastructure is much like the one we use in the old map.cgi. It
    # contains multipe references to locations (one possibly in each field).
    loc_lookup = {'ByKeyword': {}, 'ByID': {}, 'ByCode': {}}

    # We build a datastructure referencing dicts instead of Location objects
    # for easier pickling and unpickling.
    loc_dicts = [l.to_dict() for l in locations]

    print "Pretty-print location list:"
    pp = pprint.PrettyPrinter()
    pp.pprint(loc_dicts)

    print "Building lookup data structure..."
    for l in loc_dicts:
        if (('displayName' in l) and l['displayName']) or 'displayName' not in l:
            loc_lookup['ByID'][l['id']] = l

            if 'code' in l:
                loc_lookup['ByCode'][l['code'].lower()] = l

            if 'keywords' in l:
                for keyword in l['keywords'].split(' '):
                    if keyword not in loc_lookup['ByKeyword']:
                        loc_lookup['ByKeyword'][keyword] = []
                    loc_lookup['ByKeyword'][keyword].append(l)

    print "Pickling..."
    fh = open(opts.output, 'w')
    pickle.dump(loc_lookup, fh)
    fh.close()
    print "Done."

if __name__ == '__main__':
    main()
