(function flexible(window, document) {
  var mainSection = document.getElementById('main-section');  // 只作用于id为main-section的区域
  var docEl = document.documentElement;
  var dpr = window.devicePixelRatio || 1;

  // 调整 #main-section 下内容的字体大小
  function setSectionFontSize() {
    if (mainSection) {
      // 仅调整 #main-section 内的字体大小，不影响其他地方
      var elementsToAdjust = mainSection.querySelectorAll('.column, .panel, .no, .chart');
      elementsToAdjust.forEach(function(el) {
        el.style.fontSize = 12 * dpr + "px";
      });

      // 设置 #main-section 内部 nav 元素的字体大小（如果有）
      var nav = mainSection.querySelector('nav');
      if (nav) {
        nav.style.fontSize = '16px'; // 设置 nav 的字体大小，仅限于 main-section 内部的 nav
      }
    } else {
      document.addEventListener("DOMContentLoaded", setSectionFontSize);
    }
  }
  setSectionFontSize();

  // 设置 1rem = viewWidth / 24，仅针对 #main-section 的内容
  function setRemUnit() {
    var rem = docEl.clientWidth / 24;
    if (mainSection) {
      mainSection.style.fontSize = rem + "px";
    }
  }

  setRemUnit();

  // 在页面缩放时重置 rem 单位，仅针对 #main-section 的内容
  window.addEventListener("resize", setRemUnit);
  window.addEventListener("pageshow", function(e) {
    if (e.persisted) {
      setRemUnit();
    }
  });

  // 检测是否支持 0.5px 边框，仅针对 #main-section 的内容
  if (dpr >= 2) {
    var fakeBody = document.createElement("body");
    var testElement = document.createElement("div");
    testElement.style.border = ".5px solid transparent";
    fakeBody.appendChild(testElement);
    docEl.appendChild(fakeBody);
    if (testElement.offsetHeight === 1) {
      docEl.classList.add("hairlines");
    }
    docEl.removeChild(fakeBody);
  }
})(window, document);
