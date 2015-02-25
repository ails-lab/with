// require.js looks for the following global when initializing
var require = {
	baseUrl: ".",
	paths: {
		"jquery":               "bower_modules/jquery/dist/jquery.min",
		 "bridget":				"bower_modules/jquery-bridget/jquery.bridget",
		"bootstrap":            "bower_modules/components-bootstrap/js/bootstrap.min",
		"crossroads":           "bower_modules/crossroads/dist/crossroads.min",
		"hasher":               "bower_modules/hasher/dist/js/hasher.min",
		"knockout":             "bower_modules/knockout/dist/knockout",
		"knockout-projections": "bower_modules/knockout-projections/dist/knockout-projections",
		"signals":              "bower_modules/js-signals/dist/signals.min",
		"text":                 "bower_modules/requirejs-text/text",
		"slick":                "bower_modules/slick.js/slick/slick.min",
		"holder":               "bower_modules/hasherjs/dist/js/hasher.min",
		"eventie": 				"bower_modules/eventie/eventie",
	    "eventEmitter": 		"bower_modules/eventEmitter/EventEmitter",
	    "doc-ready":            "bower_modules/doc-ready/doc-ready",
	    "get-style-property":   "bower_modules/get-style-property/get-style-property",
	    "get-size":             "bower_modules/get-size/get-size",
	    "matches-selector":     "bower-modules/matches-selector/matches-selector",
	    "outlayer":             "bower-modules/outlayer/outlayer",
		"imagesloaded":         "bower_modules/imagesloaded/imagesloaded.pkgd.min",
		"masonry":              "bower_modules/masonry/dist/masonry.pkgd",
		"facebook":             "//connect.facebook.net/en_US/all",
		"knockout-validation":  "bower_modules/knockout-validation/dist/knockout.validation.min",
		"knockout-amd-helpers": "bower_modules/knockout-amd-helpers/build/knockout-amd-helpers.min"
	},
	shim: {
		"bootstrap": { deps: ["jquery"] },
		"facebook": { exports: "FB" }
	}
};
