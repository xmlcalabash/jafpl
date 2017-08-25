$(document).ready(function() {
  let r = Math.random()
  //console.log("r1=", r)
  if (r > 0.80) {
    let word = "fine"
    let dow = new Date().getDay()
    if (dow == 5 && Math.random() > 0.50) {
      word = "Friday"
    } else {
      let words = ["fantastic", "freaking", "fabulous"]
      r = Math.random()
      let index = Math.floor(r * words.length)
      //console.log("r2=", r, " index=", index)
      word = words[index]
    }
    $("#fine").text(word)
  }
});
