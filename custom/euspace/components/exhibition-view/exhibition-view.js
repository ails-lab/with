define(['knockout', 'text!./exhibition-view.html', 'app', 'magnific-popup'], function (ko, template, app, magnificPopup) {


    function initCarousel (){

        require(["js/vendor/slick.js/slick/slick.min","js/plugin"], function(slick,EUSpaceApp) {
            var EUSpaceUI=new EUSpaceApp.EUSpaceApp.ui({

            });
            EUSpaceUI.initCarousel();
            EUSpaceUI.initExpandExhibitionText();
            EUSpaceUI.initImageZoom();
        });
    };

    ko.bindingHandlers.backgroundImage = {
        update: function(element, valueAccessor) {
            ko.bindingHandlers.style.update(element,
                function(){return {backgroundImage: "url('" + valueAccessor() + "')"}});
        }
    };

    function Record(data) {
        var self = this;
        self.recordId = "";
        self.title = "";
        self.description="";
        self.thumb = "";
        self.fullres="";
        self.view_url="";
        self.source="";
        self.creator="";
        self.provider="";
        self.rights="";
        self.url="";
        self.externalId = "";
        self.isLoaded = ko.observable(false);

        self.load = function(data) {
            if(data.title==undefined){
                self.title="No title";
            }else{self.title=data.title;}
            self.url="#item/"+data.id;
            self.view_url=data.view_url;
            self.thumb=data.thumb;
            self.fullres=data.fullres;
            self.description=data.description;
            self.source=data.source;
            self.creator=data.creator;
            self.provider=data.provider;
            self.rights=data.rights;
            self.recordId=data.id;
            self.externalId=data.externalId;
        };

        self.cachedThumbnail = ko.pureComputed(function() {

            if(self.thumb){
                if (self.thumb.indexOf('/') === 0) {
                    return self.thumb;
                } else {
                    var newurl='url=' + encodeURIComponent(self.thumb)+'&';
                    return '/cache/byUrl?'+newurl+'Xauth2='+ sign(newurl);
                }}
            else{
                return "img/content/thumb-empty.png";
            }
        });
        if(data != undefined) self.load(data);
    }

    function EViewModel(params) {
        document.body.setAttribute("data-page","collection");
        var self = this;

        var $container = $(".grid");
        self.route = params.route;
        var counter = 1;
        self.exhName = ko.observable('');
        self.access = ko.observable("READ");
        self.id = ko.observable(params.id);
        self.owner = ko.observable('');
        self.ownerId = ko.observable(-1);
        self.itemCount = ko.observable(0);
        self.exhItems = ko.observableArray();
        self.desc = ko.observable('');
        self.loading = ko.observable(false);
        self.showCarousel = ko.observable(false);

        self.revealItems = function (data) {
            for (var i in data) {
                console.log(data);
                var result = data[i];
                var record = new Record({
                    id: result.dbId,
                    thumb: result.thumbnailUrl,
                    title: result.title,
                    view_url: result.sourceUrl,
                    creator: result.creator,
                    provider: result.provider,
                    source: result.source,
                    rights: result.rights,
                    externalId: result.externalId
                });
                record.annotation='';
                if (result.hasOwnProperty('exhibitionRecord')) {
                    record.annotation=result.exhibitionRecord.annotation;
                    record.videoUrl=result.exhibitionRecord.videoUrl;
                }
                var styleId = self.exhItems().length % 5 || 0;
                var styleIdMapping = {
                    0: 1,
                    1: 1,
                    2: 2,
                    3: 2,
                    4: 3
                };
                styleId = styleIdMapping[styleId];
                record.css =  'item style' + styleId;//0, 1, 2, 3, 4 -> 2 x style1, 2 x style2 , 1 x style3
                self.exhItems().push(record);
            }
            self.exhItems.valueHasMutated();
            initCarousel();
        };

        self.loadExhibition = function (id) {
            self.loading(true);
            $.ajax({
                "url": "/collection/" + self.id(),
                "method": "get",
                "contentType": "application/json",
                "success": function (data) {
                    self.exhName(data.title);
                    self.desc(data.description);
                    self.owner(data.owner);
                    self.ownerId(data.ownerId);
                    self.itemCount(data.itemCount);
                    self.access(data.access);
                    self.revealItems(data.firstEntries);
                    self.showCarousel(true);
                    self.loading(false);
                },
                error: function (xhr, textStatus, errorThrown) {
                    self.loading(false);
                    $.smkAlert({text:'An error has occured', type:'danger', permanent: true});
                }
            });
        };
        self.loadExhibition();

        self.loadNext = function () {
            self.moreItems();
        };

        self.moreItems = function () {
            if (self.loading === true) {
                setTimeout(self.moreItems(), 300);
            }
            if (self.loading() === false) {
                self.loading(true);
                var offset = self.exhItems().length;
                $.ajax({
                    "url": "/collection/" + self.id() + "/list?count=40&start=" + offset,
                    "method": "get",
                    "contentType": "application/json",
                    "success": function (data) {
                        console.log(data.itemCount);
                        self.revealItems(data.records);
                        self.loading(false);
                    },
                    "error": function (result) {
                        self.loading(false);
                    }
                });
            }
        };
    }

    return {
        viewModel: EViewModel,
        template: template
    };
});
