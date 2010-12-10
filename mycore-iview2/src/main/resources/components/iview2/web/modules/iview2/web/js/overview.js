//TODO @description und @param ergänzen, wo es noch fehlt

/**
 * @namespace
 * @name		iview
 */
var iview = iview || {};
/**
 * @namespace	Package for Overview, contains Default Overview View and Controller
 * @memberOf 	iview
 * @name		overview
 */
iview.overview = iview.overview || {};

/**
 * @class
 * @constructor
 * @version	1.0
 * @memberOf	iview.overview
 * @name	 	View
 * @description View to display with a given template the underlying model
 */
iview.overview.View = function() {
	this._mainClass;
	this._customClass;	
	this._divSize = {};
	this._previewSize = {};
	this._amount = {"width":0, "height":0};
	this._scrollBarWidth = 0;
	this._numberOfPages = -1;
	this._currentFirstRow = -1;
	this._selected = 0;
	this._visible = true;
	this._pages = [];
	this._tileUrlProvider = null;
	this._useScrollBar = true;
	this.my = null;
	this.onevent = new iview.Event(this);
};

(function() {
	/**
	 * @public
	 * @function
	 * @name		disableScrollBar
	 * @memberOf	iview.overview.View
	 * @description	disables the use of scrollbar. If the View was already created the scrollbar will be removed
	 */
	function disableScrollBar() {
		this._useScrollBar = false;
		this._scrollBarWidth = 0;
		if (this.my.bar) {
			bar.detach();
		}
	}
	
	/**
	 * @public
	 * @function
	 * @name		setDivSize
	 * @memberOf	iview.overview.View
	 * @description	sets the size of the seperate which includes the image and the related infos
	 * @param 		{object} divSize
	 * @param		{float} divSize.width holds the width of the Divs
	 * @param		{float} divSize.height holds the height of the Divs
	 */
	function setDivSize(divSize) {
		this._divSize = {'width':toInt(divSize.width),
						'height':toInt(divSize.height)};
	}

	/**
	 * @public
	 * @function
	 * @name		setPreviewSize
	 * @memberOf	iview.overview.View
	 * @description	sets the according size for every preview image div
	 * @param 		{float} previewSize
	 * @param		{float} previewSize.width width of the preview Image div
	 * @param		{float} previewSize.height height of the preview Image div
	 */
	function setPreviewSize(previewSize) {
		this._previewSize = {'width':toInt(previewSize.width),
							'height':toInt(previewSize.height)};
	}
	
	/**
	 * @public
	 * @function
	 * @name		setNumberOfPages
	 * @memberOf	iview.overview.View
	 * @description	sets the number of pages the document has
	 * @param	 	{float} value number of pages 
	 */
	function setNumberOfPages(value) {
		this._numberOfPages = toInt(value);
		if (this._numberOfPages < 0) {
			this._numberOfPages *= -1;
		}
	}
	
	/**
	 * @public
	 * @function
	 * @name		addPage
	 * @memberOf	iview.overview.View
	 * @description	adds another page to the list
	 * @param 		{integer} id holds the id of the page which is added
	 * @param		{string} href path to the image which is added
	 */
	function addPage(id, href) {
		this._pages[id] = href;
	}
		
	/**
	 * @public
	 * @function
	 * @name		resize
	 * @memberOf	iview.overview.View
	 * @description	resizes the overview when the size of the browser is changing
	 */
	function resize() {
		createContainer(this);
		posOverviewContainer(this);
		if (this._visible) {
			loadImages(this);
		}
	}
	
	/**
	 * @public
	 * @function
	 * @name		setSelected
	 * @memberOf	iview.overview.View
	 * @description	takes the given pagenumber and adapts the view in that way that the selected one is visible
	 * @param 		{integer} value pagenumber of the newly selected entry
	 */
	function setSelected(value) {
		this._selected = toInt(value);
		calculateFirstRow(this);
		if (this.my.bar) {
			this.my.barObj.setCurValue(this._currentFirstRow);
		}
	}
	
	/**
	 * @public
	 * @function
	 * @name		visible
	 * @memberOf	iview.overview.View
	 * @description	makes the View visible depending on the given boolean value, if no value is given the View will switch in the opposite mode than it's currently
	 * @param 		{boolean} bool holds the state into which the View shall switch
	 */
	function visible(bool) {
		if (typeof bool === "undefined") {
			bool = !this._visible;
		}
		if (bool == true) {
			this._visible = true;
			//we're getting displayed so show the User the latest stuff
			this.resize();
			this.my.self.css("visibility", "visible");
			this.my.self.slideDown("slow");
		} else {
			this._visible = false;
			//TODO will be no longer needed as soon as jQuery 1.4.4 is used, as this version is able to gain size for hidden elements
			var that = this.my.self;
			this.my.self.slideUp("slow", function() {that.css("display", "block")});
			this.my.self.css("visibility", "hidden");
		}
	}
	
	/**
	 * @private
	 * @function
	 * @name		loadImages
	 * @memberOf	iview.overview.View
	 * @description	load the overview so that the actually picture is in first line
	 * @param 		{instance} that
	 */	
	function loadImages(that) {
		// for later check initialized
		var delFrom = that._amount.height;		
		
		var divBox;
		// proceed line wise
		for (var i = 0; i < that._amount.height; i++) {
			for (var j = 0; j < that._amount.width; j++) {
				divBox= that.my.pictures[(i * (that._amount.width)) + j];
				//get back previously hidden div's and set the picPos it represents
				divBox.css("display", "block").attr("page",((i + that._currentFirstRow) * that._amount.width) + j);
				
				//load needed Previews
				if ((((i + that._currentFirstRow) * that._amount.width) + j) < that._numberOfPages) {
					loadSingleImage(that, divBox);
				}
				// last line who contains pages
				if ((i + that._currentFirstRow) >= (Math.floor((that._numberOfPages) / that._amount.width))) {
					// page not existing???
					if ((((that._currentFirstRow + i) * that._amount.width)+j) > (that._numberOfPages - 1)) {
						divBox.css("display", "none");
						if (i <= that._amount.height) {
							delFrom = i + 1;
						}
					}
				}
			}
		}
		// to remove redundant divs when the pagenumbers are small
		if (delFrom < that._amount.height) {
			for (var i = delFrom * that._amount.width; i < that.my.pictures.length; i++) {
				that.my.pictures[(i * (that._amount.width)) + j].css("display", "none");
			}
		}
	}
	
	/**
	 * @private
	 * @function
	 * @name		loadSingleImage
	 * @memberOf	iview.overview.View
	 * @description	load the separate pictures in the according divboxes
	 * @param 		{instance} that image that is loaded
	 * @param 		{object} divBox the according div box which contains one image
	 */
	function loadSingleImage(that, divBox) {
		var pageName = that._pages[toInt(divBox.attr("page"))+1];
		var source = that._tileUrlProvider.assembleUrl(0, 0, 0, pageName);
		var preview = jQuery(divBox.children("img")[0]);
		
		// original Values needed, because Img will scale automatic in each Props
		var origImage = new Image;
		origImage.onload = function() {trimImage(preview, source, {'height':origImage.height, 'width':origImage.width}, that);};
		origImage.src = source;
		
		// fill Info div
		var infoDiv=jQuery(divBox.children("div.infoDiv")[0]);
		infoDiv.html(pageName);
		infoDiv.attr("title", pageName);
	}
	
	/**
	 * @private
	 * @function
	 * @name		trimImage
	 * @memberOf	iview.overview.View
	 * @description	fits picture to the correct size within the divBox
	 * @param 		{object} preview image which is displayed
	 * @param 		{string} source path to the image
	 * @param		{object} orig original image
	 * @param		{instance} that
	 */
	function trimImage(preview, source, orig, that) {
		preview.attr("src", source);
	
		// scale preview-images
		var scaleFactorH = (that._previewSize.height / orig.height);
		var scaleFactorW = (that._previewSize.width / orig.width);
		
		if (scaleFactorH <= 1) {
			// image is higher then div
			if (scaleFactorW <= 1) {
				// image is wider than the div
				if (scaleFactorW < scaleFactorH) {
					preview.css("width", that._previewSize.width + "px");
					preview.css("height", orig.height * scaleFactorW + "px");
				} else {
					preview.css("width", orig.width * scaleFactorH + "px");
					preview.css("height", that._previewSize.height + "px");
				}
			} else {
				// image is smaller than the div
				preview.css("width", orig.width * scaleFactorH + "px");
				preview.css("height", that._previewSize.height + "px");
			}
		} else {
			// image is lower than the div
			if (scaleFactorW <= 1) {
				// image is wider than the div
				preview.css("width", that._previewSize.width + "px");
				preview.css("height", orig.height * scaleFactorW + "px");
			} else {
				// image is smaller than the div
				if (scaleFactorW < scaleFactorH) {
					preview.css("width", that._previewSize.width + "px");
					preview.css("height", orig.height * scaleFactorW + "px");
				} else {
					preview.css("width", orig.width * scaleFactorH + "px");
					preview.css("height", that._previewSize.height + "px");
				}
			}
		}
		
		// center previews horz & vert
		// (infoDivs are all same within with width and size)
		preview.css("left", (preview.parent().width() - preview.outerWidth(true)) / 2 + "px");
	}
	
	/**
	 * @private
	 * @function
	 * @name		calculateFirstRow
	 * @memberOf	iview.overview.View
	 * @description	if overview is already created and is called so load loadImageFromLine() and adjust scrollbar
	 * @param	 	{instance} that 
	 */
	function calculateFirstRow(that) {
		that._currentFirstRow = Math.floor((parseInt(that._selected) - 1) / that._amount.width);
		// if overview is to big for remaining pages
		if (that._currentFirstRow + that._amount.height - 1 > Math.ceil(that._numberOfPages / that._amount.width) - 1) {
			that._currentFirstRow = Math.ceil(that._numberOfPages / that._amount.width) - that._amount.height;
		}
		// if all pages fit in overview
		if (that._currentFirstRow < 0) {
			that._currentFirstRow = 0;
		}
		loadImages(that);
		// shift scrollbar to the actually start-line
		if (that._useScrollBar) {
			that.my.barObj.setCurValue(currentFirstRow);
		}
	}
	
	/**
	 * @private
	 * @function
	 * @name		createContainer
	 * @memberOf	iview.overview.View
	 * @description	creates all containers which are used for the overview (#container == #previewImages)
	 * @param 		{instance} that 
	 */
	function createContainer(that) {
		//calculate the number of horizontal and vertical div-boxes
		var el=that.my.self;
		var width = Math.floor((el.width() - that._scrollBarWidth) / that._divSize.width);
		var height = Math.floor(el.height() / that._divSize.height);
		//dont do not needed work if everything is just fine
		if (width == that._amount.width && height == that._amount.height) return;
		that._amount = {
			'width': width,
			'height': height};
		
		if (that.my.bar) {
			that.my.barObj.setMaxValue(Math.ceil(that._numberOfPages/width)-height);
			that.my.barObj.setProportion(1/Math.abs(Math.ceil(that._numberOfPages/width)-height+1));
		}
		
		//clear the old pictures if there
		jQuery(that.my.pictures).each(function(pos, element) {
			if (!element) return;//needed as the resize can happen more often than this element exists
			element.detach();
			delete that.my.pictures[pos];
		});
		// create target Div's
		for (var i = 0; i < that._amount.height; i++) {
			for (var j = 0; j < that._amount.width; j++) {
				var infoDiv = jQuery("<div>")
					.addClass("infoDiv");
				
				var prevImg = jQuery("<img>")
					.addClass("previewDiv")
					.css("cursor", "pointer");
				//adding them to the list of available containers so we can access them easily
				that.my.pictures[i*that._amount.width + j] = jQuery("<div>")
					.addClass("divBox")
					.attr("no",(i * that._amount.width) + j)
					.css("float", "left")
					.appendTo(that.my.picContainer)
					.append(infoDiv)
					.append(prevImg)
					.click(function() {
						that.onevent.notify({"new": toInt(jQuery(this).attr("page")), 'type':that.CLICK});
					});
			}
		}
	}
	
	/**
	 * @private
	 * @function
	 * @name		posOverviewContainer
	 * @memberOf	iview.overview.View
	 * @description	positions nicely the divBoxes within the available Space
	 * @param	 	{instance} that the overview object where the code is run in
	 */
	function posOverviewContainer(that) {
		that._scrollBarWidth = ((that.my.bar)? that.my.bar.outerWidth(true): 0);
	
		if (that.my.bar) {
			that.my.barObj.setSize(that.my.self.height());
		}
		that.my.picContainer.css("width", that.my.self.innerWidth() - that._scrollBarWidth);

		that.my.picContainer.css("padding", 0);//reset everything else it does subsum and we screw everything up
		that.my.picContainer.css("padding-left",
				(that.my.picContainer.innerWidth() - (that.my.pictures[0].outerWidth(true)*that._amount.width))/2 + "px");
		that.my.picContainer.css("padding-top",
				(that.my.self.innerHeight() - (that.my.pictures[0].outerHeight(true)*that._amount.height))/2 + "px");
	}
	
	/**
	 * @public
	 * @function
	 * @name		createView
	 * @memberOf	iview.overview.View
	 * @description	create the view in the overview
	 * @param	 	{object} args
	 * @param		{string} args.mainClass tells what the main Class for the scrollbar shall be
	 * @param		{string} args.customClass allows it to modify the Scrollbar in parts to differ from others
	 * @param		{String,DOM-Object,anything jQuery supports} parent DOM element to which the overview is added
	 * @param 		{string} [arguments[1]] tells the id of the overview. This property isn't needed as the
	 *  scrollbar works just fine without ids. The id maybe only needed if you plan to perform custom
	 *  transformations on the scrollbar DOM
	 */
	function createView(args) {
		this._mainClass = args.mainClass || "";
		this._customClass = args.customClass || "";
		
		var main = jQuery("<div>").addClass(this._mainClass + " " + this._customClass)
		.appendTo(args.parent)
		.css("visibility", "hidden");
		
		if (typeof arguments[1] !== "undefined") {
			main.attr("id", arguments[1]);
		}
		
		//deactivate Browser Drag&Drop
		main.mousedown(function() {return false;});
		
		var picContainer = jQuery("<div>").addClass("picContainer").appendTo(main);
		
		this.my = {'self':main, 'picContainer': picContainer, 'pictures': []};

		this._useScrollBar = args._useScrollBar;
		if (args.useScrollBar) {
			prepareScrollBar(this);
		}
		
		createContainer(this);
		posOverviewContainer(this);
		calculateFirstRow(this);
		var that = this;
		jQuery(window).resize(function() {that.resize()});
		loadImages(this);
	}
	
	/**
	 * @private
	 * @function
	 * @name		prepareScrollBar
	 * @memberOf	iview.overview.View
	 * @description	create Scrollbar in the overview
	 * @param 		{instance} that 
	 */
	function prepareScrollBar(that) {
		var scrollbar = new iview.scrollbar.Controller();
		var parent = that.my.self;
		scrollbar.createView({ 'direction': 'vertical', 'parent': parent, 'mainClass': 'scroll', 'type':'stepper'});
		scrollbar._model.onevent.attach(function(sender,args){
			if (args.type == "curVal") {
				that._currentFirstRow = args["new"];
				loadImages(that);
			}
		});
		scrollbar.setSize(parent.height());
		scrollbar.setStepByClick(1);
		scrollbar.setJumpStep(1);
		
		// register additional Events
		scrollbar.addEventFrom("mousemove", parent);
		scrollbar.addEventFrom("mouseup", parent);
		scrollbar.addEventFrom("mousescroll", parent);
		that.my.bar = jQuery(parent.find(".scrollV:first")[0]);
		that.my.barObj = scrollbar;
	}
	
	/**
	 * @public
	 * @function
	 * @name		setTileUrlProvider
	 * @memberOf	iview.overview.View
	 * @description	set the tileUrlProvider from which the tiles are taken
	 * @param 		{tileUrlProvider} provider which gives preview tiles
	 */
	function setTileUrlProvider(provider) {
		this._tileUrlProvider = provider;
	}
	
	iview.overview.View.prototype.createView = createView;
	iview.overview.View.prototype.setDivSize = setDivSize;
	iview.overview.View.prototype.resize = resize;
	iview.overview.View.prototype.setNumberOfPages = setNumberOfPages;
	iview.overview.View.prototype.setSelected = setSelected;
	iview.overview.View.prototype.visible = visible;
	iview.overview.View.prototype.addPage = addPage;
	iview.overview.View.prototype.setTileUrlProvider = setTileUrlProvider;
	iview.overview.View.prototype.setPreviewSize = setPreviewSize;
	iview.overview.View.prototype.disableScrollBar = disableScrollBar;
	iview.overview.View.prototype.CLICK = 1;
})();

/**
 * @class
 * @constructor
 * @version		1.0
 * @memberOf	iview.overview
 * @name 		Controller
 * @description Controller for Overview
 */
iview.overview.Controller = function(modelProvider, view, tileUrlProvider) {
	this._model = modelProvider.createModel();
	this._view = new (view || iview.overview.View)();
	this._tileUrlProvider = tileUrlProvider;
	var select = this._model.SELECT;
	var that = this;
	
	this._model.onevent.attach(function(sender, args) {
		if (args.type == select) {
			that._view.setSelected(args["new"]);
		}
	});
	
	this._view.onevent.attach(function(sender, args) {
		if (args.type == that._view.CLICK) {
			that._view.visible(false);
			that._model.setPosition(args["new"]+1);
		}
	});
};

(function() {
	
	/**
	 * @public
	 * @function
	 * @name		createView
	 * @memberOf	iview.overview.Controller
	 * @description	creates the view for the overview
	 * @param 		{object} args
	 * @param		{string} args.mainClass tells what the main Class for the scrollbar shall be
	 * @param		{string} args.customClass allows it to modify the Scrollbar in parts to differ from others
	 * @param		{String,DOM-Object,anything jQuery supports} parent DOM element to which the overview is added
	 * @param		{boolean} args.useScrollBar tells if the overview will use a scrollbar or not
	 */
	function createView(args) {
		this._view.setNumberOfPages(this._model.getNumberOfPages())
		this._view.setDivSize({'width':200, 'height':200});
		this._view.setPreviewSize({'width':180, 'height':160});
		this._view.setTileUrlProvider(this._tileUrlProvider);
		var iter = this._model.iterator();
		var temp;
		while (iter.hasNext()) {
			temp = iter.next();
			this._view.addPage(temp.getOrder(), temp.getHref())
		}
		this._view.createView({
			'mainClass': args.mainClass,
			'customClass':args.customClass,
			'useScrollBar':args.useScrollBar,
			'parent':args.parent});
	}
	
	/**
	 * @public
	 * @function
	 * @name		showView
	 * @memberOf	iview.overview.Controller
	 * @description	tells the view to hide itself
	 */
	function showView() {
		this._view.visible(true);
	}
	/**
	 * @public
	 * @function
	 * @name		hideView
	 * @memberOf	iview.overview.Controller
	 * @description	tells the view to hide itself
	 */	
	function hideView() {
		this._view.visible(false);
	}

	/**
	 * @public
	 * @function
	 * @name		toggleView
	 * @memberOf	iview.overview.Controller
	 * @description	tells the View to change it's display mode to the currently opposite mode
	 */
	function toggleView() {
		this._view.visible();
	}
	
	/**
	 * @public
	 * @function
	 * @name		setSelected
	 * @memberOf	iview.overview.Controller
	 * @description	takes the given pagenumber and adapts the view in that way that the selected one is visible
	 * @param 		{integer} value pagenumber of the newly selected entry 
	 */
	function setSelected(value) {
		this._view.setSelected(value);
	}
	
	/**
	 * @public
	 * @function
	 * @name		getActive
	 * @memberOf	iview.overview.Controller
	 * @description	returns the current state of the OverviewView (if its visible or not)
	 */
	function getActive() {
		return this._view._visible;
	}
	
	/**
	 * @public
	 * @function
	 * @name		attach
	 * @memberOf	iview.overview.Controller
	 * @description	attach Eventlistener to used overview model
	 */
	function attach(listener) {
		this._model.attach(listener);
	}
	
	/**
	 * @public
	 * @function
	 * @name		detach
	 * @memberOf	iview.overview.Controller
	 * @description	detach previously attached Eventlistener from overview model
	 */
	function detach(listener) {
		this._model.detach(listener);
	}
	
	iview.overview.Controller.prototype.createView = createView;
	iview.overview.Controller.prototype.showView = showView;
	iview.overview.Controller.prototype.hideView = hideView;
	iview.overview.Controller.prototype.toggleView = toggleView;
	iview.overview.Controller.prototype.setSelected = setSelected;
	iview.overview.Controller.prototype.getActive = getActive;
	iview.overview.Controller.prototype.attach = attach;
	iview.overview.Controller.prototype.detach = detach;
})();