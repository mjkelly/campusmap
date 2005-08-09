/******************************************************************
* drag.js
*
* Functions for a draggable, scrollable, dynamically loading map
* with multiple zoom levels.
*
* Copyright 2005 Michael Kelly and David Lindquist
******************************************************************/


/*
TODO:
	- reinstage bgLayer, or some method of aborting drags that go beyond
	  the window.
	- write Perl function to remove old path images

BUGS:
	- Firefox: location backgrounds sometimes do not appear until the map is dragged

COSMETIC DEFECTS:
	- Opera: Opera counts obscured layers in page sizing. This makes the scroll
	  bar jump around, and scrolling via arrow keys a strange experience.
	- Firefox: Periodic flashes when scrolling via arrow keys.
	- Firefox: Path image repeats if path layer is too big
	  (background-repeat: no-repeat; doesn't work?).

*/

// size of the map squares
var squareWidth = 200;
var squareHeight = 200;

// variables for doing dragging
var origX = 0;
var origY = 0;
var origXdraggy = 0;
var origYdraggy = 0;

// the draggable layer
var draggy;
// this is just draggy.style -- save ourselves one dereference ;)
var draggyStyle;
// a <div> we use to for miscelleneous debug output
//var indicator;

var locations;
var map;
var path;

// is this IE?
var IE = false;

// current X/Y offset of the viewport, in pixels against the current map
var view;

var arrowPan = 10;
// flags for tracking the up/down status of arrow keys
var arrowLeft = false;
var arrowUp = false;
var arrowRight = false;
var arrowDown = false;

// used to track the function that handles key repepetition
var repeatID = 0;

// variables for the pan buttons
var buttonID = 0;
var buttonScrollCount = 0;
var buttonDelay = 15;
var buttonIncrement = 10;

// for tracking zoom levels: view.curZoom is an index into zoomLevels, which is
// an array of ZoomLevel objects
var zoomLevels;
var scales = new Array(1, 0.5, 0.25, 0.125);

var locationList = new Array();
var pathObj;

/******************************************************************
* Initialize the map: set key handlers, initialize global variables, create
* zoom levels and the zoom control bar, etc. This function should be called
* first (possibly after external global variable initialization), otherwise
* things will break.
******************************************************************/
function basicInit(){

	draggy = document.getElementById("draggy");
	draggyStyle = draggy.style;
	map = document.getElementById("map");
	locations = document.getElementById("locations");
	path = document.getElementById("path");
	//indicator = document.getElementById("indicator");

	// Check if this is IE. This screens out Opera, because Opera
	// is NOT IE, despite what it says. Sheesh. (Is this a kluge or
	// what?!)
	IE = document.all && (navigator.userAgent.indexOf("Opera") == -1);

	/**
	 * Set the handelers
	 *
	 **/

	// We use onkeydown and onkeyup due to browser incompatabilities with onkeypress
	document.onkeydown = handleKeyDown;
	document.onkeyup = handleKeyUp;

	draggy.onmousedown = handleMouseDown;
	//document.getElementById("bgLayer").onmouseout = handleMouseOut;
	document.onmouseup = handleMouseUp;

	// Create the zoomLevel objects.  
	// Setup for the zoom levels
	zoomLevels = new Array(
		new ZoomLevel('map', 1,     36, 33, 7200, 6600),
		new ZoomLevel('map', 0.5,   18, 17, 3600, 3300),
		new ZoomLevel('map', 0.25,  9,  9,  1800, 1650),
		new ZoomLevel('map', 0.125, 5,  5,  900,  825 )
	);
	//view.curZoom = 2; // this is an index into zoomLevels

	// build the zoom buttons
	var zoomHTML = "";
	for(var i = 0; i < zoomLevels.length; i++){
		zoomHTML += '<a href="javascript:setZoom(' + i + ')"><img id="zoomButton_' + i + '" src="'
			+ zoomButtonURL
			+ '" width="30" height="20" border="0" /></a><br />';
	}
	document.getElementById("zoomSelect").innerHTML = zoomHTML;
}

/******************************************************************
* Mark the location of the depress for later use.
* Start monitoring mouse movements.
******************************************************************/
function handleMouseDown(e){
	// For compatability with IE (other browsers use parameter)
	if(!e) { var e = window.event; }

	// save the location of the click for later comparisons with move events
	origX = e.clientX;
	origY = e.clientY;

	// also save the current position of the draggable object.
	origXdraggy = parseInt(draggy.style.left+0);
	origYdraggy = parseInt(draggy.style.top+0);

	// Start monitoring mouse movement
	document.onmousemove = handleMouseMove;
}

/******************************************************************
* Stop monitoring mouse movements
******************************************************************/
function handleMouseUp(e){
	// For compatability with IE (other browsers use parameter)
	if(!e) var e = window.event;  // unused!

	//indicator.innerHTML = "Up";
	
	// Stop monitoring mouse movement
	document.onmousemove = null;
}

/******************************************************************
* Decides if mouse has gotten out of the browser window...if so, 
* do the same thing as if the mouse button was released
******************************************************************/
function handleMouseOut(e){
	if(!e) var e = window.event;

	//alert(e.relatedTarget);
	if(!e.relatedTarget)
		handleMouseUp(e);  // no need to pass e

}

/******************************************************************
* Handle dragging -- this only called if the mouse button is
* already down.
******************************************************************/
function handleMouseMove(e){
	if(!e) var e = window.event;

	// calculate how far the mouse has moved from its
	// initial click position, and add that to the
	// draggable object's initial position
	view.curX = -(origXdraggy + (e.clientX - origX));
	view.curY = -(origYdraggy + (e.clientY - origY));
	
	updateMapLocation();
	view.checkForLoad();
}

/******************************************************************
* KeyDown event handler. Fakes key repeating by setting flags for
* the different movement keys.
******************************************************************/
function handleKeyDown(e){
	if(!e) var e = window.event;
	// get the key code, no matter which browser we're in
	// (thanks to QuirksMode.com)
	var code;
	if (e.keyCode)
		code = e.keyCode;
	else if (e.which)
		code = e.which;

	// if an arrow key is pressed, set that key's flag, and set launchRepeater
	// (which says to launch the key-repeat function)
	var launchRepeater = false;
	switch(code) {
	
		case 37: // left
			if(arrowLeft)
				return;
			arrowLeft = true;
			//indicator.innerHTML =  "Left Arrow DOWN";
			launchRepeater = true;
			break;
			
		case 38: // up
			if(arrowUp)
				return;
			arrowUp = true;
			//indicator.innerHTML =  "Up Arrow DOWN";
			launchRepeater = true;
			break;
			
		case 39: // right
			if(arrowRight)
				return;
			arrowRight = true;
			//indicator.innerHTML =  "Right Arrow DOWN";
			launchRepeater = true;
			break;
			
		case 40: // down
			if(arrowDown)
				return;
			arrowDown = true;
			//indicator.innerHTML =  "Down Arrow DOWN";
			launchRepeater = true;
			break;
			
		default:
			//indicator.innerHTML =  "Other DOWN";
			
	}

	// if an arrow key was pressed and we aren't alreadying running keyRepeater,
	// launch it
	if(launchRepeater && !repeatID)
		repeatID = setInterval("keyRepeater()", 30);
	
	return true;
}

/******************************************************************
* KeyUp event handler. Clears arrow flags when arrow keys are released.
* keyRepeater() handles its own demise.
******************************************************************/
function handleKeyUp(e){
	if(!e) var e = window.event;
	var code;
	// For most browsers
	if (e.keyCode)
		code = e.keyCode;
	// Netscape?
	else if (e.which)
		code = e.which;

	switch(code) {
	
		case 37: // left
			arrowLeft = false;
			//indicator.innerHTML =  "Left Arrow UP";
			break;
			
		case 38: // up
			arrowUp = false;
			//indicator.innerHTML =  "Up Arrow UP";
			break;
			
		case 39: // right
			arrowRight = false;
			//indicator.innerHTML =  "Right Arrow UP";
			break;
			
		case 40: // down
			arrowDown = false;
			//indicator.innerHTML =  "Down Arrow UP";
			break;
			
		default:
			//indicator.innerHTML =  "Other UP";
			
	}
	
	// stop the key repeater if no arrow keys still down
	if( !arrowLeft && !arrowRight && !arrowUp && !arrowDown ){
		clearInterval(repeatID);  // Stop repeating
		repeatID = 0;  // stop the repeatID from being double set in handleKeyDown
	}

	return true;
}


/******************************************************************
* The "scroll left" button was pressed. Left = 0.
******************************************************************/
function panLeft ()
{
	view.loadView(view.curX - view.width/2, view.curY);
	if(!buttonID){
		buttonScrollCount = 30;
		buttonID = setInterval("scrollRepeater(" + -buttonIncrement + ", 0)", buttonDelay);
	}
}

/******************************************************************
* The "scroll up" button was pressed. Up = 1.
******************************************************************/
function panUp ()
{
	view.loadView(view.curX, view.curY - view.width/2);
	if(!buttonID){
		buttonScrollCount = 30;
		buttonID = setInterval("scrollRepeater(0, " + -buttonIncrement + ")", buttonDelay);
	}
}

/******************************************************************
* The "scroll right" button was pressed. Right = 2.
******************************************************************/
function panRight ()
{
	view.loadView(view.curX + view.width/2, view.curY);
	if(!buttonID){
		buttonScrollCount = 30;
		buttonID = setInterval("scrollRepeater(" + buttonIncrement + ", 0)", buttonDelay);
	}
}

/******************************************************************
* The "scroll down" button was pressed. Down = 3.
******************************************************************/
function panDown ()
{
	view.loadView(view.curX, view.curY + view.width/2);
	if(!buttonID){
		buttonScrollCount = 30;
		buttonID = setInterval("scrollRepeater(0, " + buttonIncrement + ")", buttonDelay);
	}
}

/******************************************************************
* Repeatedly move the viewport the given amounts.
******************************************************************/
function scrollRepeater(dx, dy)
{
	view.curX += dx;
	view.curY += dy;
	
	buttonScrollCount--;
	updateMapLocation();
	
	// Hackish way to check for load halfway through 
	// and when we're done
	if(buttonScrollCount%15 == 0)
		view.checkForLoad();
		
	// after this function has repeated enough times, clear it
	if(buttonScrollCount <= 0)
	{
		clearInterval(buttonID);
		buttonID = 0;
	}
}

/******************************************************************
* The "zoom in" button was pressed.
******************************************************************/
function zoomIn(){
	if(view.curZoom > 0)
		view.setZoomLevel(view.curZoom - 1);

	//alert("Current: (" + view.curX + ", " + view.curY + "), zoom: " + view.curZoom);
}

/******************************************************************
* The "zoom out" button was pressed.
******************************************************************/
function zoomOut(){
	if(view.curZoom < zoomLevels.length - 1)
		view.setZoomLevel(view.curZoom + 1);

	//alert("Current: (" + view.curX + ", " + view.curY + "), zoom: " + view.curZoom);
}

/******************************************************************
* The "center" button was pressed.
******************************************************************/
function centerView(){
	view.recenter();
	// wasn't that easy?
}

/******************************************************************
* One of the specific zoom level buttons was pressed.
* DEPRECATED.
******************************************************************/
function handleSetZoom(e){
	if(!e) var e = window.event;

	var target;
	if (e.target)
		target = e.target; // Netscape
	else if (e.srcElement)
		target = e.srcElement; // IE
	if (target.nodeType == 3) // defeat Safari bug
		target = target.parentNode;


	var id = parseInt(target.id.substr(target.id.length - 1, 1));

	view.setZoomLevel(id);
}

/******************************************************************
* Set the zoom level of the main view. This is a wrapper, accessed directly
* from the buttons.
******************************************************************/
function setZoom(i){
	view.setZoomLevel(i);
}

/******************************************************************
* To be run from a setInterval(), this function fakes key repetition
* by moving the screen while an arrow key's flag is set.
* 
* handleKeyDown() and handleKeyUp() take care of starting and
* stopping this function.
******************************************************************/
function keyRepeater(){
	
	if(arrowLeft){
		view.curX -= arrowPan;
		arrowRight = false;
	}
	if(arrowUp){
		view.curY -= arrowPan;
		arrowDown = false;
	}
	if(arrowRight){
		view.curX += arrowPan;
		arrowLeft = false;
	}
	if(arrowDown){
		view.curY += arrowPan;
		arrowUp = false;
	}
	
	// update the viewport, load any new grid squares necessary
	updateMapLocation();
	view.checkForLoad();
}

/******************************************************************
* Apply the current X and Y to draggy.  This causes the map to move.
* If view.curX or Y goes past the boundary, map is set to boundary.
******************************************************************/
function updateMapLocation(){
	// Map bound checking
	if(view.curX < 0){ view.curX = 0; }
	if(view.curY < 0){ view.curY = 0; }
	if(view.curX > zoomLevels[view.curZoom].getMaxX()){ view.curX = zoomLevels[view.curZoom].getMaxX(); }
	if(view.curY > zoomLevels[view.curZoom].getMaxY()){ view.curY = zoomLevels[view.curZoom].getMaxY(); }

	// Set to negatives to preserve our sanity in other places
	// e.g: move map right==> set to the negative change to cause
	// draggy to move to the left
	draggyStyle.left = -view.curX + "px";
	draggyStyle.top  = -view.curY + "px";
}

/******************************************************************
* This function updates path objects -- that is, the main path object and its
* two terminal locations -- on the screen. Needed when changing zoom levels.
******************************************************************/
function updatePathObjects(){
	var str = "";

	// update all locations
	for(var i in locationList){
		str += '<div class="location" style="left: ' + locationList[i].x*scales[view.curZoom] + 'px;'
			+ 'top: ' + locationList[i].y*scales[view.curZoom] + 'px;">'
			+ '<span id="locButton_' + i + '"></span>'
			+ '<div class="locationLabel" id="location_' + i + '">'
				+ locationList[i].name
			+ '</div>'
			//+ '<div style="background: transparent; position: absolute; left: 0px; right: 0px; top: 0px; bottom: 0px;"></div>'
			+ "</div>\n";
	}
	//alert(str);
	locations.innerHTML = str;

	// update the path
	if( pathObj && pathObj.distance != 0 ){
		var min;
		var max;
		if(pathObj.source > pathObj.destination){
			min = pathObj.destination;
			max = pathObj.source;
		}
		else{
			min = pathObj.source;
			max = pathObj.destination;
		}

		str = '<div class="path" style="left: ' + pathObj.x*scales[view.curZoom] + 'px;'
			+ 'top: ' + pathObj.y*scales[view.curZoom] + 'px; width: '
			+ pathObj.width*scales[view.curZoom] + 'px; height: '
			+ pathObj.height*scales[view.curZoom] + 'px; background: transparent url(\''
			+ pathObj.images[view.curZoom].src + '\');"></div>';
		path.innerHTML = str;
		//alert(str);
	}
}

/******************************************************************
* This is the ZoomLevel class.
* It is really just a struct that contains all the information associated
* with a single zoom level: map size in pixels and grid squares,
* scale multiplier, etc.
******************************************************************/
function ZoomLevel(name, zoom, gridX, gridY, pixX, pixY){
	// The zoom scale of the map (e.g. 1, .5, .25, .125)
	this.mapZoom = zoom;

	// The name of the map (e.g. "map")
	this.mapName = name;

	// # of grid squares on the map (e.g. 36x33)
	this.gridMaxX = gridX;
	this.gridMaxY = gridY;
	
	// Resolution of the map (7200x6600)
	this.mapMaxX = pixX;
	this.mapMaxY = pixY;
	//alert("New ZoomLevel: (" + this.mapMaxX + ", " + this.mapMaxY + ")");

	this.getMaxX = function() {
		return this.mapMaxX - view.width;
	}
	this.getMaxY = function() {
		return this.mapMaxY - view.height;
	}
}

/******************************************************************
* This is the Location class.
******************************************************************/
function Location(name, x, y, index){
	this.name = name;
	this.x = x;
	this.y = y;

	//caller is responsible for getting the key right
	locationList[index] = this;
}

/******************************************************************
* Center on the coordinates of the Location at the given index.
******************************************************************/
function centerOnLocation(index){
	view.slideTo(locationList[index].x, locationList[index].y);
}

/******************************************************************
* This is the Path class.
* It preloads all the images associated with the given path at contructor time,
* then acts as a struct, storing info about the path.
******************************************************************/
function Path(x, y, width, height, dist, src, dst){
	this.x = x;
	this.y = y;
	this.width = width;
	this.height = height;

	// these are not set until later (from getPaths)
	this.distance = dist;
	this.source = src;
	this.destination = dst;

	//alert("Path ctor with: " + x + ', ' + y + ', ' + width + ', ' + height + ', ' + src + ', ' + dst);

	// set this object as the global path object
	pathObj = this;

	// keep track of the smaller and larger location IDs
	var min; var max;
	if(pathObj.source > pathObj.destination){ min = this.destination; max = this.source; }
	else{ min = this.source; max = this.destination; }

	// preload all the images at this zoom level
	this.images = new Array(zoomLevels.length);
	for(var i = 0; i < zoomLevels.length; i++){
		this.images[i] = new Image(width * zoomLevels[i].mapZoom, height * zoomLevels[i].mapZoom);
		this.images[i].src = pathsDir + '/im-' + min + '-' + max + '-' + i + '.png';
		//alert(this.images[i].src);
	}

	// redraw the screen
	updatePathObjects();
}

/******************************************************************
* This is the Viewport class.
* It controls the main view, and includes functions to move, change zoom
* levels, etc. UI-level functions (like panLeft(), zoomIn(), etc) are not here
* -- they are global functions.
******************************************************************/
// Constructor: 
function Viewport(x, y, width, height, curZoom){
	var zoomFactor = scales[curZoom] / scales[0];

	this.width = width;
	this.height = height;

	// default* values are what we zoom to when the "center" button is clicked
	this.defaultX = x;
	this.defaultY = y;
	this.defaultZoom = curZoom;

	// X/Y location of the last place loadView() was run
	this.lastLoadX = 0;
	this.lastLoadY = 0;

	// there's only one Viewport at any one time, and it's stored in the
	// global variable 'view'
	view = this;

	// initialize the sizes of viewport's container
	document.getElementById("mapContainer").style.width = view.width + "px";
	document.getElementById("mapContainer").style.height = view.height + "px";

	/******************************************************************
	* Snap to the given absolute coordinates.
	******************************************************************/
	this.centerOn = function(x, y){
		this.curX = x*scales[this.curZoom] - view.width/2;
		this.curY = y*scales[this.curZoom] - view.height/2;
		updateMapLocation();
		updatePathObjects();
		view.loadCurrentView();
	}

	/******************************************************************
	* Slide smoothly to the given absolute coordinates.
	******************************************************************/
	this.slideTo = function(x, y){
		var targX = x*scales[this.curZoom] - view.width/2;
		var targY = y*scales[this.curZoom] - view.height/2;

		var diffX = targX - this.curX;
		var diffY = targY - this.curY;

		// if it's too far, just snap
		if( Math.sqrt(diffX*diffX + diffY*diffY) > view.width*1.5){
			this.centerOn(x, y);
		}
		else{
			var dx = diffX / 30;
			var dy = diffY / 30;

			if(!buttonID){
				buttonScrollCount = 30;
				buttonID = setInterval("scrollRepeater(" + dx + ", " + dy + ")", buttonDelay);
			}
			else{
				// XXX: eventually, here, we should slide smoothly into
				// our own scroll
				buttonScrollCount = 0;	// interrupt the scroll in progress
			}
		}
	}

	/******************************************************************
	* Go back to the coordinates and zoom level given at construction time.
	* Slide smoothly if we don't have to change zoom levels, else snap.
	******************************************************************/
	this.recenter = function(){
		if(this.curZoom == this.defaultZoom){
			this.slideTo(this.defaultX, this.defaultY);
		}
		else{
			this.setZoomLevelNoRedraw(this.defaultZoom);
			this.centerOn(this.defaultX, this.defaultY);
		}
	};

	/******************************************************************
	* Set the zoom level to the given value. Do not reload the view.
	* (Use this if you're calling centerOn() or something immediately
	* afterwards.
	******************************************************************/
	this.setZoomLevelNoRedraw = function(newZoom){
			// bounds checking. be paranoid.
			if(newZoom < 0){
				alert("setZoomLevel: Error! New zoom level = " + newZoom + " < 0 (too small).");
				newZoom = 0;
			}
			else if (newZoom >= zoomLevels.length){
				alert("setZoomLevel: Error! New zoom level  = " + newZoom + " >= zoomLevels.length = " + zoomLevels.length + " (too big).");
				newZoom = zoomLevels.length - 1;
			}

			// abort before we do anything serious if we're not
			// actually _chaning_ zoom levels
			if(newZoom == this.curZoom){
				// update the zoom button graphics
				document.getElementById("zoomButton_" + newZoom).src = zoomButtonSelectedURL;
				return;
			}
			else{
				// update the zoom button graphics
				document.getElementById("zoomButton_" + newZoom).src = zoomButtonSelectedURL;
				document.getElementById("zoomButton_" + this.curZoom).src = zoomButtonURL;
			}

			// store the ratio between the new zoom and the old one
			var zoomFactor = scales[newZoom] / scales[this.curZoom];
			this.curZoom = newZoom;
			
			// adjust the viewport offsets so the center remains the same
			this.curX += this.width/2;
			this.curY += this.height/2;

			this.curX *= zoomFactor;
			this.curY *= zoomFactor;

			this.curX -= this.width/2;
			this.curY -= this.height/2;

			//alert("new zoom level = " + newZoom);
	}

	/******************************************************************
	* Set the zoom level to the given value, and automatically reload the
	* view.
	******************************************************************/
	this.setZoomLevel = function(newZoom){
		this.setZoomLevelNoRedraw(newZoom);
		// load new squares, and update the viewport
		view.loadCurrentView();
		updateMapLocation();
		updatePathObjects();
	}

	/******************************************************************
	* Load the squares at the given pixel offset, with the given
	* viewport dimensions.
	******************************************************************/
	this.loadView = function(x, y){

		//alert("LoadView: " + x + ", " + y);

		// the grid coordinates of the upper-left corner
		var initialX = parseInt(x/squareWidth);
		var initialY = parseInt(y/squareHeight);

		// the width and height of the viewport, in grid squares --
		// plus a buffer so we load a bit more than necessary
		var gridWidth = parseInt(this.width/squareWidth + 2);
		var gridHeight = parseInt(this.height/squareHeight + 2);

		//alert("loadView(" + x + ", " + y + ")");

		var str = "";

		// loop through all grid squares that are within the current view, or
		// close to the current view
		for(var numX = initialX -1; numX <= (gridWidth + initialX); numX++) {
			for(var numY = initialY -1; numY <= (gridHeight + initialY); numY++) {
				if( numX >= 0 && numY >= 0
					&& numX < zoomLevels[view.curZoom].gridMaxX
					&& numY < zoomLevels[view.curZoom].gridMaxY) {

					str += '<div class="mapBox" style="background-image: url(\''
						+ gridDir + '/' + zoomLevels[view.curZoom].mapName + '-' + view.curZoom
						+ '[' + numY + '][' + numX + '].png\'); '
						+ 'left: ' + numX*(squareWidth) + 'px;'
						+ 'top: ' + numY*(squareHeight) + 'px;'
						+ '" ondrag="return false;"></div>' + "\n";
						//    ^^^^^<-- little-known trick! (for IE, as always) :)
					
					//alert(str);

					//str += addImage(numX, numY);
				}
			}
		}
		//alert(str);
		map.innerHTML = str;

		this.lastLoadX = x;
		this.lastLoadY = y;
		//alert(map.innerHTML);
	}

	/******************************************************************
	* Call loadView on the current viewport offset and size.
	******************************************************************/
	this.loadCurrentView = function(){
		this.loadView(this.curX, this.curY);
	}

	/******************************************************************
	* Checks to see if view.curX or view.curY has changed by a threshold
	* from the last point that the map view was completely loaded.
	******************************************************************/
	this.checkForLoad = function(){
		if(Math.abs(this.curX - this.lastLoadX) > this.width/2 ||
			Math.abs(this.curY - this.lastLoadY) > this.height/2)
		{
			this.loadCurrentView();
		}
	}

	this.curZoom = 0;
	this.setZoomLevelNoRedraw(curZoom);
	this.centerOn(x, y);

}

/******************************************************************
* Recalculate how long it takes a person to walk The Path.
* Bonus feature: Reimplements part of printf.
******************************************************************/
function calcTime(oldDist, mpm){
	// this is really what we're here to do:
	var dist = oldDist*mpm;
	var distStr = String(dist);
	var precision = 2;

	// change the mpm value in the main input form, so subsequent submits
	// use the new value
	document.main.mpm.value = mpm;

	// It _should_ be as easy as an sprintf call here, but does Javascript have that? No.
	// Instead, we go through a whole song and dance to get "%.02f" formatting.
	var parts = distStr.split(".");

	// first part as an int
	var high = (+parts[0]);

	// if we have digits below the decimal point...
	if(parts.length > 1){
		// I have the feeling I'm re-implementing part of printf...
		//alert(parts[1]);

		// cut it down to a 2-digit number with decimal places
		var small = (+parts[1]) / Math.pow(10, parts[1].length - precision);

		// round off the decimal places
		small = Math.round(small);

		// convert back to a string
		var smallStr = String(small);

		// pad with 0s
		if(smallStr.length < 2)
			smallStr = '0' + smallStr;
		// if we rounded up to 100, cut off the leading 1, and
		// increment the high place-value part
		else if(smallStr.length > 2){
			high += (+smallStr.substr(0, 1));
			smallStr = smallStr.substr(1, 2);
		}
			

		document.getElementById("time").innerHTML = high + '.' + smallStr;
	}
	else{
		document.getElementById("time").innerHTML = distStr;
	}

	// if this was called from an onsubmit event, don't submit!
	return false;
}
