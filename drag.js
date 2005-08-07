// globals dealing with properties of the base images
var squareWidth = 200;
var squareHeight = 200;


// globals dealing with dragging
var mouseDown = false;
var origX = 0;
var origY = 0;
var origXdraggy = 0;
var origYdraggy = 0;
var draggy;
var indicator;
var IE = false;

var curX = 0;
var curY = 0;

var lastLoadX = 0;
var lastLoadY = 0;

var viewPortWidth = 500;
var viewPortHeight = 375;

var draggyStyle;

var arrowPan = 10;
var arrowLeft = false;
var arrowUp = false;
var arrowRight = false;
var arrowDown = false;

// used to track the function that handles key repepetition
var tid = 0;
var buttonDone = 0;
var buttonScrollCount = 0;
var buttonDelay = 15;

var curZoom = 2;
var zoomLevels;
var scales = new Array(1, 0.5, 0.25, 0.125);

// Mark the location of the depress for later use
// Start monitoring mouse movements
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

// Stop monitoring mousemovements
function handleMouseUp(e){
	// For compatability with IE (other browsers use parameter)
	if(!e) var e = window.event;  // unused!

	indicator.innerHTML = "Up";
	mouseDown = false;
	
	// Stop monitoring mouse movement
	document.onmousemove = null;
}

// Decides if mouse has gotten out of the browser window...if so, 
// do the same thing as if the mouse button was depressed
function handleMouseOut(e){
	if(!e) var e = window.event;

	//alert(e.relatedTarget);
	if(!e.relatedTarget)
		handleMouseUp(e);  // no need to pass e

}

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

function handleButtonUp (e)
{
	loadView(curX, curY - viewPortWidth/2,  viewPortWidth, viewPortHeight);
	if(!buttonDone)
		buttonDone = setInterval("buttonTrack(1)", buttonDelay);

}
function handleButtonDown (e)
{
	loadView(curX, curY + viewPortWidth/2,  viewPortWidth, viewPortHeight);
	if(!buttonDone)
		buttonDone = setInterval("buttonTrack(3)", buttonDelay);
}

function handleButtonLeft (e)
{
	loadView(curX - viewPortWidth/2, curY,  viewPortWidth, viewPortHeight);
	if(!buttonDone)
		buttonDone = setInterval("buttonTrack(0)", buttonDelay);
}
function handleButtonRight (e)
{
	loadView(curX + viewPortWidth/2, curY,  viewPortWidth, viewPortHeight);
	if(!buttonDone)
		buttonDone = setInterval("buttonTrack(2)", buttonDelay);
}

function buttonTrack(direction)
{
	var increment = 10;
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
	if(buttonScrollCount > 30)
	{
		clearInterval(buttonDone);
		buttonScrollCount = 0;
		buttonDone = 0;
	}
}

function handleButtonZoomOut(e){
	if(curZoom < zoomLevels.length - 1){
		curZoom++;
		var zoomFactor = scales[curZoom] / scales[curZoom - 1];

		curX += viewPortWidth/2;
		curY += viewPortHeight/2;

		curX *= zoomFactor;
		curY *= zoomFactor;

		curX -= viewPortWidth/2;
		curY -= viewPortHeight/2;

		loadView(curX, curY, viewPortWidth, viewPortHeight);
		updateMapLocation();
	}

	//alert("Current: (" + curX + ", " + curY + "), zoom: " + curZoom);
}

function handleButtonZoomIn(e){
	if(curZoom > 0){
		curZoom--;
		var zoomFactor = scales[curZoom] / scales[curZoom + 1];

		curX += viewPortWidth/2;
		curY += viewPortHeight/2;

		curX *= zoomFactor;
		curY *= zoomFactor;

		curX -= viewPortWidth/2;
		curY -= viewPortHeight/2;

		loadView(curX, curY, viewPortWidth, viewPortHeight);
		updateMapLocation();
	}

	//alert("Current: (" + curX + ", " + curY + "), zoom: " + curZoom);
}

// Apply the current X and Y to draggy.  This causes the map to move.
// If curX or Y goes past the boundary, map is set to boundary.
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

// Checks to see if curX or curY has changed by a threshold
// from the last point that the map view was completely loaded
function checkForLoad(){
	if(Math.abs(curX - lastLoadX) > viewPortWidth/2 ||
		Math.abs(curY - lastLoadY) > viewPortHeight/2)
	{
		//loadView(curX - (curX % squareWidth), curY - (curY % squareHeight), viewPortWidth, viewPortHeight);
		loadView(curX,curY, viewPortWidth, viewPortHeight);
	}
}

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
	
	// Load the initial view
	loadView(curX + viewPortWidth/2, curY + viewPortHeight/2, viewPortWidth, viewPortHeight);

}

function handleKeyDown(e){
	if(!e) var e = window.event;
	var code;
	if (e.keyCode)
		code = e.keyCode;
	else if (e.which)
		code = e.which;

	switch(code) {
		case 37: // left
			if(arrowLeft)
				return;
			arrowLeft = true;
			indicator.innerHTML =  "Left Arrow DOWN";
			break;
		case 38: // up
			if(arrowUp)
				return;
			arrowUp = true;
			indicator.innerHTML =  "Up Arrow DOWN";
			break;
		case 39: // right
			if(arrowRight)
				return;
			arrowRight = true;
			indicator.innerHTML =  "Right Arrow DOWN";
			break;
		case 40: // down
			if(arrowDown)
				return;
			arrowDown = true;
			indicator.innerHTML =  "Down Arrow DOWN";
			break;
		default:
			indicator.innerHTML =  "Other DOWN";
	}

	if(!tid)
		tid = setInterval("keyRepeater()", 30);
}

function keyRepeater(){
	var cont = false;
	if(arrowLeft){
		curX -= arrowPan;
		arrowRight = false;
		cont = true;
	}
	if(arrowUp){
		curY -= arrowPan;
		arrowDown = false;
		cont = true;
	}
	if(arrowRight){
		curX += arrowPan;
		arrowLeft = false;
		cont = true;
	}
	if(arrowDown){
		curY += arrowPan;
		arrowUp = false;
		cont = true;
	}
	updateMapLocation();

	checkForLoad();

	// if non of the arrows are down...
	if(!cont){
		clearInterval(tid);  // Stop repeating
		tid = 0;  // stop the tid from being double set in handleKeyDown
	}

}

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
}

function loadView(x, y, width, height){

	var initialX = parseInt(x/squareWidth);
	var initialY = parseInt(y/squareHeight);

	var gridWidth = parseInt(width/squareWidth + 2);
	var gridHeight = parseInt(height/squareHeight + 2);

	//alert("loadView(" + x + ", " + y + ")");

	var str = "";

	for(var numX = initialX -1; numX <= (gridWidth + initialX); numX++) {
		for(var numY = initialY -1; numY <= (gridHeight + initialY); numY++) {
			if( numX >= 0 && numY >= 0 && numX < zoomLevels[curZoom].gridMaxX && numY < zoomLevels[curZoom].gridMaxY) {

				str += '<div class="mapBox" style="background: url(\''
					+ 'grid/' + zoomLevels[curZoom].mapName + '-' + curZoom + '[' + numY + '][' + numX + '].png\'); '
					+ 'left: ' + numX*(squareWidth) + 'px; top: ' + numY*(squareHeight) + 'px;'
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


// This is the ZoomLevel class.
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
