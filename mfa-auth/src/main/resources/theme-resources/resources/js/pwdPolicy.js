/* global _messages, _passwordPolicy, zxcvbn */
;(function () {
  const css = {
    error: 'alert-error pf-c-alert pf-m-inline pf-m-danger',
    success: 'alert-success pf-c-alert pf-m-inline pf-m-success',
    errorIcon: 'fa fa-fw fa-exclamation-circle',
    successIcon: 'fa fa-fw fa-check-circle'
  }

  document.head.append(h('style', {}, `
    .pwd-info {
      padding: 0 0.5em;
      display: flex;
      align-items: baseline;
    }
    .pwd-info.ok {
      color: #151515;
      color: var(--pf-global--Color--100);
    }
    .pwd-info-icon {}
    .pwd-info-title {
      flex-grow: 1;
    }
    #passwordPolicy .pf-c-alert {
      display: block;
      grid-template-areas: none;
      grid-template-columns: none;
      grid-template-rows: none;
    }
    #passwordPolicy .pf-c-alert.pf-m-inline {
      display: block;
    }
    #passwordPolicy .pf-c-alert__title {
      padding: 0;
    }
    .pwd-strength {
      padding: 10px 0 20px 0;
    }
    .pwd-strength-score { 
      font-weight: bold; text-transform: uppercase;
    }
    .pwd-strength-meter {
      display: inline-block;
      height: 5px;
      width: 100%;
      background: none;
      background-color: rgba(0, 0, 0, 0.1);
    }
    .pwd-strength-value {
      display: inline-block;
      height: 5px;
    }
  `))

  const $pwdnew = document.getElementById('password-new')
  const $pwdconfirm = document.getElementById('password-confirm')
  const $input = document.querySelector('input[type=submit]')
  const $info = document.getElementById('passwordPolicy')
  const $alert = document.querySelector('#kc-content-wrapper > .alert-error')
  const RE_MSG_PWD = /^((?:[^:,]+)[:,])\s/
  const STRENGTH = [
    'None',
    'Poor',
    'Weak',
    'Good',
    'Strong'
  ]
  const STRENGTH_COLOR = [
    '',
    'var(--pf-global--danger-color--100)',
    'var(--pf-global--warning-color--100)',
    'var(--pf-global--success-color--100)',
    'var(--pf-global--success-color--200)'
  ]

  $input.disabled = true

  $pwdnew.addEventListener('input', function (ev) {
    render()
  })
  $pwdconfirm.addEventListener('input', function (ev) {
    render()
  })

  if (!$alert) render()

  function clearAlerts () {
    $alert && $alert.remove()
    document.querySelectorAll('input[aria-invalid=true]').forEach($ => {
      $.setAttribute('aria-invalid', '')
    })
    const confirmMsg = document.getElementById('input-error-password-confirm')
    confirmMsg && confirmMsg.remove()
  }

  function render () {
    clearAlerts()
    $info.innerHTML = null
    const pwd = $pwdnew.value
    const info = validate(pwd)
    const valid = info.valid
    $input.disabled = !valid || (pwd !== $pwdconfirm.value)
    const childs = info.map(({ msg, valid }) =>
      h('span', { className: 'pwd-info' + (valid ? ' ok' : '') }, msg)
    )
    if (childs.length) {
      h($info, {},
        h('div', { className: valid ? css.success : css.error }, [
          h('div', { className: 'pf-c-alert__icon' }, [
            h('span', { className: valid ? css.successIcon : css.errorIcon }),
            h('span', { className: 'pf-c-alert__title kc-feedback-text' }, childs)
          ])
        ])
      )
    }
    renderScore(pwd)
  }

  function h (e, p, c) {
    const $ = typeof e === 'string' ? document.createElement(e) : e
    Object.entries(p).forEach(([k, v]) => {
      if (k.indexOf('on') === 0) {
        $.addEventListener(k.substring(2).toLowerCase(), v)
      } else if (k === 'style') {
        Object.entries(v).forEach(([p, v]) => { $[k][p] = v })
      } else {
        $[k] = v
      }
    })
    ;[].concat(c).forEach(c => { c != null && $.append(c) })
    return $
  }

  function clamp (min, val, max) {
    return Math.min(max, Math.max(min, val || 0))
  }

  function renderScore (pwd) {
    const zxcvbnF = (typeof zxcvbn !== 'undefined')
      ? zxcvbn
      : () => ({})

    const score = clamp(0, zxcvbnF(pwd).score, 4)
    console.log(score)

    const $meter = h('div', { className: 'col-xs-12 col-sm-12 col-md-12 col-lg-12 pwd-strength' }, [
      h('span', { }, [
        (_messages.pwdStrength || 'Password Strength:') + ' ',
        h('span', { className: 'pwd-strength-score' }, _messages['pwdMeterScore' + score] || STRENGTH[score])
      ]),
      h('span', { className: 'pwd-strength-meter' },
        h('span', {
          className: 'pwd-strength-value',
          style: {
            width: score * 25 + '%',
            backgroundColor: STRENGTH_COLOR[score]
          }
        })
      )
    ])

    $info.appendChild($meter)
  }

  function replace (info) {
    return info.replace(RE_MSG_PWD, '')
  }

  function validate (value) {
    const o = {
      lowerCase: 0,
      upperCase: 0,
      digits: 0,
      specialChars: 0,
      length: value.length
    }
    for (let i = 0; i < value.length; i++) {
      const c = value.charAt(i)
      if (/[a-z]/.test(c)) {
        o.lowerCase++
      } else if (/[A-Z]/.test(c)) {
        o.upperCase++
      } else if (/[0-9]/.test(c)) {
        o.digits++
      } else if (/[^a-zA-Z0-9\s]/.test(c)) {
        o.specialChars++
      }
    }
    const info = []
    info.valid = true
    Object.keys(o).forEach(function (key) {
      if (!_passwordPolicy[key]) return
      const msg = h('span', { className: 'pwd-info-title' }, ' ' + replace(_messages[key].replace('{0}', _passwordPolicy[key])))
      let valid = true
      if (_passwordPolicy[key] > o[key]) {
        info.valid = valid = false
      }
      const icon = h('span', { className: valid ? 'pwd-info-icon fa fa-fw fa-check' : 'fa fa-fw fa-times' })
      info.push({ valid, msg: [icon, msg] })
    })
    if (info.length) {
      info.unshift({ valid: true, msg: _messages.title })
    }
    return info
  }
})()
