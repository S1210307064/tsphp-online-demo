function compile(){
    var tsphpId='tsphp', phpId='php', consoleId='console';
    var tsphp = $('#' + tsphpId).val().trim();
    if(tsphp){
        $.ajax({
          type: 'POST',
          url: 'demo',
          data: 'tsphp='+tsphp,
          success: function(response){
                if(response!=null && response.console!=null){
                    if(response.error==null){
                        $('#' +  phpId).val(response.php);
                        $('#' + consoleId).val(response.console);
                    }else{
                        alert(response.error);
                    }
                }else{
                    alert('An unexpected error occurred, response from the server is corrupt. Please try again.')
                }
          },
          error: function(XMLHttpRequest, textStatus, errorThrown) {
             alert('An unexpected error occurred, please try again.\n: ' + textStatus + ' / ' + errorThrown);
          }
        });
    }else {
        alert('Please provide some code, otherwise it is quite boring ;)');
    }
    return false;
}