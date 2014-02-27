function compile(){
    var tsphpId='tsphp', phpId='php', consoleId='console';
    var tsphp = $('#' + tsphpId).val().trim();
    if(tsphp){
        $.ajax({
          type: 'POST',
          url: 'compile',
          data: 'tsphp='+tsphp,
          success: function(response){
                if(response != null && (response.console != null || response.error != null)){
                    if(response.error==null){
                        $('#' +  phpId).val(response.php);
                        $('#' + consoleId).val(response.console);
                    }else{
                        alert('Server reported an error:\n' + response.error);
                    }
                }else{
                    alert('An unexpected error occurred, response from the server is corrupt. Please try again.')
                }
          },
          error: function(jqXHR, status, errorThrown) {
              $('#' + consoleId).val('An unexpected error occurred: ' + errorThrown +', please try again. Response:\n'
               + jqXHR.responseText);
          }
        });
    }else {
        alert('Please provide some code, otherwise it is quite boring ;)');
    }
    return false;
}