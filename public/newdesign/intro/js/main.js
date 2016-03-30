
// ready
$( document ).ready(function(){

	// new window
	$( '#newwindow' ).on( 'click', function( e ){

		// get path
		var path = $( '#leframe' ).attr( 'src' );

		// open
		window.open(path,'_blank');
	});

	// on nav click
	$( '.nav-sidebar li a' ).on( 'click', function( e ){

		// prevent
		e.preventDefault();

		// get
		var anchor = $( this ).attr( 'data-frame' );

		// set
		$( '#leframe' ).attr( 'src', anchor);

		// this
		$( '.nav-sidebar li' ).removeClass( 'active' );
		$( this ).parent().addClass( 'active' );
	});

	// adjust viewport
	resizeViewport();

	// update framesize
	updateFramesize();

	// adjust modal iframe
	resizeModal();

	// on window change
	$( window ).resize( function(){

		// adjust viewport
		resizeViewport();

		// update frame size
		updateFramesize();

		// adjust modal iframe
		resizeModal();
	});
	
	// Load the apropiate title and development version of the page
	// from readme.md file
 	var mdFile = "site/changelog.html";

 	// method to get title from readme.md
 	// !important : title is located at line #1
	function getTitle(inputMd){
	    $.get(inputMd,function(txt){
	        var lines = txt.split("\n");
	        // get title and put it to apropiate location
	        $("header .container > h1").html(lines[0]);
	    }); 
	}

	// method to get development version of the website
	// !important : latest version is located at line #3
	function getVersion(inputMd){
	    $.get(inputMd,function(txt){
	        var lines = txt.split("\n");
	        // get version 
	        $(".version").html(lines[1]);
	    }); 
	}

	// get the title and version 
	getTitle(mdFile);
	getVersion(mdFile);

});

// method helper to resize modal iframe 
function resizeModal () {

	// get window size 
	var frameHeight = $( window ).height();

	// resize
	$( '.modal iframe' ).css( "height",frameHeight+'px' );

}

// method helper to resize view mode
function resizeViewport() {

	// get window size height
	var frameHeight = $( window ).height() - $( '.navbar' ).height();

	// resize 
	$( '.main iframe' ).css( "height",frameHeight+'px' );
}

// function to update framesize info
function updateFramesize() {

	// get window width
	var width = $( '#leframe' ).width(),
		height = $( '#leframe' ).height();

	// set
	$( '.framesize' ).html( width+"x"+height);
}

