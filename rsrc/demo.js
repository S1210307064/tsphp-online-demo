/*
 * This file is part of the TSPHP project published under the Apache License 2.0
 * For the full copyright and license information, please have a look at LICENSE in the
 * root folder or visit the project's website http://tsphp.ch/wiki/display/TSPHP/License
 */

/*
 * some function are copied and the copyright belong to the corresponding author:
 * - allowTabChar - copyright by Tim Down
 * -- copied from http://stackoverflow.com/questions/4379535/tab-within-a-text-area-without-changing-focus-jquery
 * -- changed tab to 4 spaces
 * -- included check that "tab" is not included if shift is hold
 * - setCursorPosition - copyright by HRJ
 * -- copied from http://stackoverflow.com/questions/499126/jquery-set-cursor-position-in-text-area
 * - getCursorPosition - copyright by Maximilian Ruta
 * -- copied from http://stackoverflow.com/questions/1891444/how-can-i-get-cursor-position-in-a-textarea
 */


function tsphpDemo(options){
    var settings = $.extend({
        tsphpId: 'tsphp',
        phpId: 'php',
        consoleId: 'console',
        moreInfoId: 'moreInfo',
        moreInfoTeaserId: 'moreInfoTeaser',
        feedbackId: 'feedback',
        horizontalId: 'horizontal',
        showFeedbackAfter: 3
    }, options );   
    
    var showMoreInfo=true;
    var compileCounter=0;

    var functions = {
        toggleMoreInfo : 
            function(){
                if(showMoreInfo){
                    showMoreInfo=false;
                    $('#' + settings.moreInfoTeaserId).html('<b>Click again to hide this information</b>');
                    $('#' + settings.moreInfoId).show();
                    $('#' + settings.horizontalId).split().refresh();
                } else {
                    showMoreInfo=true;
                    $('#' + settings.moreInfoTeaserId).html('Click here for further information and help');
                    $('#' + settings.moreInfoId).hide();
                    $('#' + settings.horizontalId).split().refresh();
                }
            },
        compile: 
            function(){
                var tsphp = $('#' + settings.tsphpId).val().trim();
                if(tsphp){
                    $.ajax({
                      type: 'POST',
                      url: 'compile',
                      data: 'tsphp='+encodeURIComponent(tsphp),
                      success: function(response){
                            if(response != null && (response.console != null || response.error != null)){
                                if(response.error == null){
                                    $('#' + settings.phpId).val(response.php);
                                    $('#' + settings.consoleId).val(response.console);
                                }else{
                                    alert('Server reported an error:\n' + response.error);
                                }
                            }else{
                                alert('An unexpected error occurred, response from the server is corrupt. Please try again.')
                            }
                      },
                      error: function(jqXHR, status, errorThrown) {
                          $('#' + settings.consoleId).val('An unexpected error occurred: ' + errorThrown +', please try again. Response:'
                           + jqXHR.responseText);
                      }
                    });
                }else {
                    alert('Please provide some code, otherwise it is quite boring ;)');
                }
                return false;
            },
        
    };
    
        
    $('#' + settings.moreInfoTeaserId).click(function(){
        functions.toggleMoreInfo();
    });
    
    $('#' + settings.tsphpId).keydown(function(event){
            var c = String.fromCharCode(event.which).toLowerCase();
            if ((event.ctrlKey || event.metaKey) && c == 'g'){
                functions.compile();
                ++compileCounter;
                if(compileCounter == settings.showFeedbackAfter){
                    $('#' + settings.feedbackId).slideDown({duration:350, step: function(){
                        $('#' + settings.horizontalId).split().refresh();
                        }});
                }
                event.preventDefault();
                return false;
            }
            return true;
    });
    
    $('#' + settings.tsphpId).allowTabChar();
    
    
    return functions;
} 

(function($) {
    function pasteIntoInput(el, text) {
        el.focus();
        if (typeof el.selectionStart == "number") {
            var val = el.value;
            var selStart = el.selectionStart;
            el.value = val.slice(0, selStart) + text + val.slice(el.selectionEnd);
            el.selectionEnd = el.selectionStart = selStart + text.length;
        } else if (typeof document.selection != "undefined") {
            var textRange = document.selection.createRange();
            textRange.text = text;
            textRange.collapse(false);
            textRange.select();
        }
    }

    function allowTabChar(el) {
        $(el).keydown(function(e) {
            if (e.which == 9 && !e.shiftKey) {
                pasteIntoInput(this, "    ");
                return false;
            } else if(e.which == 9 && e.shiftKey) {
                var curPos = $(el).getCursorPosition();
                if(curPos>=4){
                    $(el).setCursorPosition(curPos-4);
                }else{
                    $(el).setCursorPosition(0);
                }
                return false;
            }
        });

        // For Opera, which only allows suppression of keypress events, not keydown
        $(el).keypress(function(e) {
            if (e.which == 9) {
                return false;
            }
        });
    }

    $.fn.allowTabChar = function() {
        if (this.jquery) {
            this.each(function() {
                if (this.nodeType == 1) {
                    var nodeName = this.nodeName.toLowerCase();
                    if (nodeName == "textarea" || (nodeName == "input" && this.type == "text")) {
                        allowTabChar(this);
                    }
                }
            })
        }
        return this;
    }

    $.fn.setCursorPosition = function(pos) {
      this.each(function(index, elem) {
        if (elem.setSelectionRange) {
          elem.setSelectionRange(pos, pos);
        } else if (elem.createTextRange) {
          var range = elem.createTextRange();
          range.collapse(true);
          range.moveEnd('character', pos);
          range.moveStart('character', pos);
          range.select();
        }
      });
      return this;
    };

    $.fn.getCursorPosition = function() {
        var el = $(this).get(0);
        var pos = 0;
        if('selectionStart' in el) {
            pos = el.selectionStart;
        } else if('selection' in document) {
            el.focus();
            var Sel = document.selection.createRange();
            var SelLength = document.selection.createRange().text.length;
            Sel.moveStart('character', -el.value.length);
            pos = Sel.text.length - SelLength;
        }
        return pos;
    };
})(jQuery);

