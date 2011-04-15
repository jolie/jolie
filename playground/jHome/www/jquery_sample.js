// JavaScript Document

$(document).ready(function(){

//Code for example A
$("input.buttonAsize").click(function(){ alert($("div.contentToChange").find("p").size()); });
//show code example A
$("a.codeButtonA").click(function(){$("pre.codeA").toggle()});

//Code for example B
$("input.buttonBslidedown").click(function(){ $("div.contentToChange").find("p.fourthparagraph:hidden").slideDown("slow"); });
$("input.buttonBslideup").click(function(){ $("div.contentToChange").find("p.fourthparagraph:visible").slideUp("slow"); });
//show code example B
$("a.codeButtonB").click(function(){$("pre.codeB").toggle()});

//Code for example C
$("input.buttonCAdd").click(function(){$("div.contentToChange").find("p").not(".alert").append("<strong class=\"addedtext\">&nbsp;This text was just appended to this paragraph</strong>")});
$("input.buttonCRemove").click(function(){$("strong.addedtext").remove()});
//show code example C
$("a.codeButtonC").click(function(){$("pre.codeC").toggle()});

//Code for example D
$("input.buttonDhide").click(function(){ $("div.contentToChange").find("p.fifthparagraph").hide("slow"); });
//show code example D
$("a.codeButtonD").click(function(){$("pre.codeD").toggle()});

//Code for example E
$("input.buttonEitalics").click(function(){ $("div.contentToChange").find("em").css({color:"#993300", fontWeight:"bold"}); });
//show code example E
$("a.codeButtonE").click(function(){$("pre.codeE").toggle()});

//Code for example F
$("input.buttonFaddclass").click(function(){ $("p.firstparagraph").addClass("changeP"); });
$("input.buttonFremoveclass").click(function(){ $("p.firstparagraph").removeClass("changeP"); });
//show code example F
$("a.codeButtonF").click(function(){$("pre.codeF").toggle()});
});