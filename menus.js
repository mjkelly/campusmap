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
function menuInit(fromID, toID, fromListID, toListID){
	m_from = document.getElementById(fromID);
	m_to = document.getElementById(toID);
	m_fromList = document.getElementById(fromListID);
	m_toList = document.getElementById(toListID);
	updateList();

	m_fromList.onchange = updateText;
	m_toList.onchange = updateText;
}

/* set the text from the menu selection */
function updateText(){
	m_from.value = m_fromList.value;
	m_to.value = m_toList.value;
}

/* set the menu selection from the text */
function updateList(){
	m_fromList.value = m_from.value;
	m_toList.value = m_to.value;
}
