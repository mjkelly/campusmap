/*-----------------------------------------------------------------
* menus.js
*
* Run the location selection menus.
*
* Copyright 2005 Michael Kelly and David Lindquist
* 
* $Id$
-----------------------------------------------------------------*/

var m_from;
var m_fromList;
var m_to;
var m_toList;

/* run this first, after the menus have loaded */
function menuInit(fromID, toID, fromListID, toListID, fromTxt, toTxt){
	m_from = document.getElementById(fromID);
	m_to = document.getElementById(toID);
	m_fromList = document.getElementById(fromListID);
	m_toList = document.getElementById(toListID);
	updateListWithText(fromTxt, toTxt);

	m_fromList.onchange = updateFromText;
	m_toList.onchange = updateToText;
}

/* set the text from the menu selection (from) */
function updateFromText(){
	m_from.value = m_fromList.value;
}

/* set the text from the menu selection (to) */
function updateToText(){
	m_to.value = m_toList.value;
}

/* set the menu selection from the text */
function updateList(){
	m_fromList.value = m_from.value;
	m_toList.value = m_to.value;
}

/* set the menu selection to the given values */
function updateListWithText(fromTxt, toTxt){
	m_fromList.value = fromTxt;
	m_toList.value = toTxt;
}
