#!/usr/bin/python
# -----------------------------------------------------------------
# offline-make-pickled-locations.py -- Parses the output of
# offline-location-info.pl and generates a pickled python data structure. This
# is the same data structure as parse-locations.py, but from a different data
# source.
# Mon Aug 30 23:30:13 EDT 2010
# -----------------------------------------------------------------

import os
import sys
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
        for attr in self.valid_attributes:
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

def make_dict(loc):
    """Do a simple key-name transformation on loc.
    
    This takes a location hash with names in the style of
    offline-location-info.py and puts it in the format of locations XML file
    (which we use internall in the AppEngine CampusMap).
    """
    return {'x': loc['x'],
            'y': loc['y'],
            'name': loc['Name'],
            'id': loc['ID'],
            'aliases': loc['Aliases'],
            'keywords': loc['Keywords'],
            'code': loc['BuildingCode']}

def main():
    if len(sys.argv) < 2:
        raise Exception("USAGE: %s output-file\n"
                        "Reads output of offline-location-info.py on STDIN."
                        % sys.argv[0])

    output_file = sys.argv[1]
    print "output_file = %s" % output_file

    lines = sys.stdin.readlines()

    print "read %d lines" % len(lines)

    loc_dicts = []
    rec = {'Aliases': []}
    for line in lines:
        line = line.rstrip()
        #print 'line = <%s>' % line
        if line == '':
            # new record
            # if rec['Keywords']:
            #     rec['Keywords'] = rec['Keywords'].split(' ')
            # else:
            #     rec['Keywords'] = []
            #print "new record: %s" % rec
            loc_dicts.append(rec)
            rec = {'Aliases': []}
        else:
            k,v = line.split('=', 2)
            if k == 'Aliases':
                rec['Aliases'].append(v)
            else:
                rec[k] = v

    print "%d locations" % len(loc_dicts)
    loc_dicts = [make_dict(loc) for loc in loc_dicts]
    #pp = pprint.PrettyPrinter()
    #pp.pprint(loc_dicts)
    #print loc_dicts
    for l in loc_dicts:
        print "%s %s" % (l['id'], l['name'])

    loc_lookup = {'ByKeyword': {}, 'ByID': {}, 'ByCode': {}}
    print "Building lookup data structure..."
    for l in loc_dicts:
        loc_lookup['ByID'][l['id']] = l

        if 'code' in l:
            loc_lookup['ByCode'][l['code'].lower()] = l

        if 'keywords' in l:
            for keyword in l['keywords'].split(' '):
                if keyword not in loc_lookup['ByKeyword']:
                    loc_lookup['ByKeyword'][keyword] = []
                loc_lookup['ByKeyword'][keyword].append(l)

    print "Pickling..."
    fh = open(output_file, 'w')
    pickle.dump(loc_lookup, fh)
    fh.close()
    print "Done."

if __name__ == '__main__':
    main()
