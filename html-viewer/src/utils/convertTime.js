module.exports = (time) => {
  let ms = time % 1000;
  time = (time - ms) / 1000;
  let secs = time % 60;
  time = (time - secs) / 60;
  let mins = time % 60;

  return mins + ':' + secs + '.' + ms;
};