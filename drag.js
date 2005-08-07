/******************************************************************
* drag.js
*
* Functions for a draggable, scrollable, dynamically loading map
* with multiple zoom levels.
*
* Copyright 2005 Michael Kelly and David Lindquist
******************************************************************/

// size of the map squares
var squareWidth = 200;
var squareHeight = 200;

// size of the map view
var viewPortWidth = 500;
var viewPortHeight = 375;

// variables for doing dragging
var mouseDown = false;
var origX = 0;
var origY = 0;
var origXdraggy = 0;
var origYdraggy = 0;

// the draggable layer
var draggy;
// this is just draggy.style -- save ourselves one dereference ;)
var draggyStyle;
// a <div> we use to for miscelleneous debug output
var indicator;

// is this IE?
var IE = false;

// current X/Y offset of the viewport, in pixels against the current map
var curX = 0;
var curY = 0;

// X/Y location of the last place loadView() was run
var lastLoadX = 0;
var lastLoadY = 0;

var arrowPan = 10;
// flags for tracking the up/down status of arrow keys
var arrowLeft = false;
var arrowUp = false;
var arrowRight = false;
var arrowDown = false;

// used to track the function that handles key repepetition
var repeatID = 0;

// variables for the pan buttons
var buttonDone = 0;
var buttonScrollCount = 0;
var buttonDelay = 15;

// for tracking zoom levels: curZoom is an index into zoomLevels, which is
// an array of ZoomLevel objects
var curZoom;
var zoomLevels;
var scales = new Array(1, 0.5, 0.25, 0.125);

/******************************************************************
* Initialize the map: set key handlers, set initial layer positions,
* initialize global variables, etc.
******************************************************************/
function dragInit(){

	draggy = document.getElementById("draggy");
	draggyStyle = draggy.style;
	indicator = document.getElementById("indicator");

	// Check if this is IE. This screens out Opera, because Opera
	// is NOT IE, despite what it says. Sheesh. (Is this a kluge or
	// what?!)
	IE = document.all && (navigator.userAgent.indexOf("Opera") == -1);

	// move the draggable element to the default location
	draggyStyle.left = "0px"
	draggyStyle.top = "0px"

	document.getElementById("container").style.width = viewPortWidth + "px";
	document.getElementById("container").style.height = viewPortHeight + "px";

	/**
	 * Set the handelers
	 *
	 **/

	// We use onkeydown and onkeyup due to browser incompatabilities with onkeypress
	document.onkeydown = handleKeyDown;
	document.onkeyup = handleKeyUp;

	draggy.onmousedown = handleMouseDown;
	document.getElementById("bgLayer").onmouseout = handleMouseOut;
	document.onmouseup = handleMouseUp;
	document.getElementById("buttonUp").onclick = handleButtonUp;
	document.getElementById("buttonDown").onclick = handleButtonDown;
	document.getElementById("buttonLeft").onclick = handleButtonLeft;
	document.getElementById("buttonRight").onclick = handleButtonRight;

	document.getElementById("buttonZoomIn").onclick = handleButtonZoomIn;
	document.getElementById("buttonZoomOut").onclick = handleButtonZoomOut;

	// Create the zoomLevel objects.  
	// Setup for the zoom levels
	zoomLevels = new Array(
		new ZoomLevel('map', 1,     36, 33, 7200, 6600),
		new ZoomLevel('map', 0.5,   18, 17, 3600, 3300),
		new ZoomLevel('map', 0.25,  9,  9,  1800, 1650),
		new ZoomLevel('map', 0.125, 5,  5,  900,  825 )
	);
	curZoom = 2; // this is an index into zoomLevels
	
	// Load the initial view
	loadView(curX + viewPortWidth/2, curY + viewPortHeight/2, viewPortWidth, viewPortHeight);

}

/******************************************************************
* Mark the location of the depress for later use.
* Start monitoring mouse movements.
******************************************************************/
function handleMouseDown(e){
	// For compatability with IE (other browsers use parameter)
	if(!e) { var e = window.event; }

	// Debugging
	indicator.innerHTML = "Down @ (" + e.clientX + ", " + e.clientY + ")";

	// XXX: Necessary?  We set document.onmousemove.
	// Mark the mouse as being down for later checking
	mouseDown = true;

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

	indicator.innerHTML = "Up";
	mouseDown = false;
	
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

	// XXX: is this boolean check needed anymore?  handleMouseMove function
	// is only available when mouseDown
	// if the mouse is down, we're dragging
	if(mouseDown){

		// calculate how far the mouse has moved from its
		// initial click position, and add that to the
		// draggable object's initial position
		curX = -(origXdraggy + (e.clientX - origX));
		curY = -(origYdraggy + (e.clientY - origY));
		
		updateMapLocation();

		//draggy.moveTo(-curX, -curY);
		//document.layers["draggy"].moveTo(-curX, -curY);
		
		indicator.innerHTML = "Current @ (" + curX + ", " + curY + ")" + 
				      "  Last Load @ (" + lastLoadX + ", " + lastLoadY + ")";
		
		checkForLoad();
	}
}

/******************************************************************
* KeyDown event handler. Fakes key repeteating by setting flags for
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
			indicator.innerHTML =  "Left Arrow DOWN";
			launchRepeater = true;
			break;
			
		case 38: // up
			if(arrowUp)
				return;
			arrowUp = true;
			indicator.innerHTML =  "Up Arrow DOWN";
			launchRepeater = true;
			break;
			
		case 39: // right
			if(arrowRight)
				return;
			arrowRight = true;
			indicator.innerHTML =  "Right Arrow DOWN";
			launchRepeater = true;
			break;
			
		case 40: // down
			if(arrowDown)
				return;
			arrowDown = true;
			indicator.innerHTML =  "Down Arrow DOWN";
			launchRepeater = true;
			break;
			
		default:
			indicator.innerHTML =  "Other DOWN";
			
	}

	// if an arrow key was pressed and we aren't alreadying running keyRepeater,
	// launch it
	if(launchRepeater && !repeatID)
		repeatID = setInterval("keyRepeater()", 30);
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
			indicator.innerHTML =  "Left Arrow UP";
			break;
			
		case 38: // up
			arrowUp = false;
			indicator.innerHTML =  "Up Arrow UP";
			break;
			
		case 39: // right
			arrowRight = false;
			indicator.innerHTML =  "Right Arrow UP";
			break;
			
		case 40: // down
			arrowDown = false;
			indicator.innerHTML =  "Down Arrow UP";
			break;
			
		default:
			indicator.innerHTML =  "Other UP";
			
	}
	
	// stop the key repeater if no arrow keys still down
	if( !arrowLeft && !arrowRight && !arrowUp && !arrowDown ){
		clearInterval(repeatID);  // Stop repeating
		repeatID = 0;  // stop the repeatID from being double set in handleKeyDown
	}
}


/******************************************************************
* The "scroll left" button was pressed. Left = 0.
******************************************************************/
function handleButtonLeft (e)
{
	loadView(curX - viewPortWidth/2, curY,  viewPortWidth, viewPortHeight);
	if(!buttonDone)
		buttonDone = setInterval("buttonTrack(0)", buttonDelay);
}

/******************************************************************
* The "scroll up" button was pressed. Up = 1.
******************************************************************/
function handleButtonUp (e)
{
	loadView(curX, curY - viewPortWidth/2,  viewPortWidth, viewPortHeight);
	if(!buttonDone)
		buttonDone = setInterval("buttonTrack(1)", buttonDelay);

}

/******************************************************************
* The "scroll right" button was pressed. Right = 2.
******************************************************************/
function handleButtonRight (e)
{
	loadView(curX + viewPortWidth/2, curY,  viewPortWidth, viewPortHeight);
	if(!buttonDone)
		buttonDone = setInterval("buttonTrack(2)", buttonDelay);
}

/******************************************************************
* The "scroll down" button was pressed. Down = 3.
******************************************************************/
function handleButtonDown (e)
{
	loadView(curX, curY + viewPortWidth/2,  viewPortWidth, viewPortHeight);
	if(!buttonDone)
		buttonDone = setInterval("buttonTrack(3)", buttonDelay);
}

/******************************************************************
* Repeatedly move the viewport in the given direction:
* left = 0, up = 1, right = 2, down = 3 (keycode order).
******************************************************************/
function buttonTrack(direction)
{
	var increment = 10;
	
	// check which direction to move
	switch (direction)
	{
		case 0:
			curX -= increment;
			break;
			
		case 1:
			curY -= increment;
			break;
			
		case 2:
			curX += increment;
			break;
			
		case 3:
			curY += increment;
			break;
	}
	
	buttonScrollCount++;
	updateMapLocation();
	
	// Hackish way to check for load halfway through 
	// and when we're done
	if(buttonScrollCount%15 == 0)
		checkForLoad();
		
	// after this function has repeated enough times, clear it
	if(buttonScrollCount > 30)
	{
		clearInterval(buttonDone);
		buttonScrollCount = 0;
		buttonDone = 0;
	}
}

/******************************************************************
* The "zoom in" button was pressed.
******************************************************************/
function handleButtonZoomIn(e){
	if(curZoom > 0){
		curZoom--;
		var zoomFactor = scales[curZoom] / scales[curZoom + 1];

		// adjust the viewport offsets so the center remains the same
		curX += viewPortWidth/2;
		curY += viewPortHeight/2;

		curX *= zoomFactor;
		curY *= zoomFactor;

		curX -= viewPortWidth/2;
		curY -= viewPortHeight/2;

		// load new squares, and update the viewport
		loadView(curX, curY, viewPortWidth, viewPortHeight);
		updateMapLocation();
	}

	//alert("Current: (" + curX + ", " + curY + "), zoom: " + curZoom);
}

/******************************************************************
* The "zoom out" button was pressed.
******************************************************************/
function handleButtonZoomOut(e){
	if(curZoom < zoomLevels.length - 1){
		curZoom++;
		var zoomFactor = scales[curZoom] / scales[curZoom - 1];
		
		// adjust the viewport offsets so the center remains the same
		curX += viewPortWidth/2;
		curY += viewPortHeight/2;

		curX *= zoomFactor;
		curY *= zoomFactor;

		curX -= viewPortWidth/2;
		curY -= viewPortHeight/2;
		
		// load new squares, and update the viewport
		loadView(curX, curY, viewPortWidth, viewPortHeight);
		updateMapLocation();
	}

	//alert("Current: (" + curX + ", " + curY + "), zoom: " + curZoom);
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
		curX -= arrowPan;
		arrowRight = false;
	}
	if(arrowUp){
		curY -= arrowPan;
		arrowDown = false;
	}
	if(arrowRight){
		curX += arrowPan;
		arrowLeft = false;
	}
	if(arrowDown){
		curY += arrowPan;
		arrowUp = false;
	}
	
	// update the viewport, load any new grid squares necessary
	updateMapLocation();
	checkForLoad();
}

/******************************************************************
* Apply the current X and Y to draggy.  This causes the map to move.
* If curX or Y goes past the boundary, map is set to boundary.
******************************************************************/
function updateMapLocation(){
	// Map bound checking
	if(curX < 0){ curX = 0; }
	if(curY < 0){ curY = 0; }
	if(curX > zoomLevels[curZoom].mapMaxX){ curX = zoomLevels[curZoom].mapMaxX; }
	if(curY > zoomLevels[curZoom].mapMaxY){ curY = zoomLevels[curZoom].mapMaxY; }

	// Set to negatives to preserve our sanity in other places
	// e.g: move map right==> set to the negative change to cause
	// draggy to move to the left
	draggyStyle.left = -curX + "px";
	draggyStyle.top  = -curY + "px";
}

/******************************************************************
* Load the squares at the given pixel offset, with the given
* viewport dimensions.
******************************************************************/
function loadView(x, y, width, height){

	// the grid coordinates of the upper-left corner
	var initialX = parseInt(x/squareWidth);
	var initialY = parseInt(y/squareHeight);

	// the width and height of the viewport, in grid squares --
	// plus a buffer so we load a bit more than necessary
	var gridWidth = parseInt(width/squareWidth + 2);
	var gridHeight = parseInt(height/squareHeight + 2);

	//alert("loadView(" + x + ", " + y + ")");

	var str = "";

	// loop through all grid squares that are within the current view, or
	// close to the current view
	for(var numX = initialX -1; numX <= (gridWidth + initialX); numX++) {
		for(var numY = initialY -1; numY <= (gridHeight + initialY); numY++) {
			if( numX >= 0 && numY >= 0
				&& numX < zoomLevels[curZoom].gridMaxX
				&& numY < zoomLevels[curZoom].gridMaxY) {

				str += '<div class="mapBox" style="background: url(\''
					+ 'grid/' + zoomLevels[curZoom].mapName + '-' + curZoom
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
	draggy.innerHTML = str;

	lastLoadX = x;
	lastLoadY = y;
	//alert(draggy.innerHTML);
}

/******************************************************************
* Checks to see if curX or curY has changed by a threshold
* from the last point that the map view was completely loaded.
******************************************************************/
function checkForLoad(){
	if(Math.abs(curX - lastLoadX) > viewPortWidth/2 ||
		Math.abs(curY - lastLoadY) > viewPortHeight/2)
	{
		loadView(curX,curY, viewPortWidth, viewPortHeight);
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
	this.mapMaxX = pixX - viewPortWidth;
	this.mapMaxY = pixY - viewPortHeight;
	//alert("New ZoomLevel: (" + this.mapMaxX + ", " + this.mapMaxY + ")");
}
