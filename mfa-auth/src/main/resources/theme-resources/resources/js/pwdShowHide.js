;(function () {
  function h (e, p, c) {
    const $ = document.createElement(e)
    Object.entries(p).forEach(([k, v]) => {
      if (k.indexOf('on') === 0) {
        $.addEventListener(k.substring(2).toLowerCase(), v)
      } else if (k === 'style') {
        Object.entries(v).forEach(([p, v]) => { $[k][p] = v })
      } else {
        $[k] = v
      }
    })
    ;[].concat(c).forEach(c => { c && $.append(c) })
    return $
  }

  function eye (onClick) {
    let show = false
    const getCN = () => show ? 'fa fa-eye-slash' : 'fa fa-eye'

    const inner = h('i', {
      className: getCN(),
      onclick: () => {
        show = !show
        inner.className = getCN()
        onClick(show)
      }
    })

    return h('span', {
      className: 'pwd-show-hide'
    }, inner)
  }

  document.head.append(h('style', {}, `
    .pwd-show-hide{
      position: absolute;
      right: 2.5em;
      top: 1.5em;
      transform: translateY(-50%);
    }
    .pwd-show-hide i{
      font-size: 1.7em;
      color: var(--pf-global--BorderColor--200);
      cursor: pointer;
      display: block;
    }
  `))

  document.querySelectorAll('input[type=password]').forEach($ => {
    if ($.style.display === 'none') return
    $.style.paddingRight = '2.5em'
    const onClick = (show) => {
      $.type = show ? 'text' : 'password'
    }
    $.insertAdjacentElement('afterend', eye(onClick))
  })
})()
