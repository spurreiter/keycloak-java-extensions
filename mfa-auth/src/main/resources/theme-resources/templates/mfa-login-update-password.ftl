<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('password','password-confirm'); section>
    <#if section = "header">
        ${msg("updatePasswordTitle")}
    <#elseif section = "form">
        <style>
        #policies { padding: 0.5em 0; }
        </style>
        <form id="kc-passwd-update-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <input type="text" id="username" name="username" value="${username}" autocomplete="username"
                   readonly="readonly" style="display:none;"/>
            <input type="password" id="password" name="password" autocomplete="current-password" style="display:none;"/>

            <div id="passwordPolicy">
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="password-new" class="${properties.kcLabelClass!}">${msg("passwordNew")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <input type="password" id="password-new" name="password-new" class="${properties.kcInputClass!}"
                           autofocus autocomplete="new-password"
                           aria-invalid="<#if messagesPerField.existsError('password','password-confirm')>true</#if>"
                    />

                    <#if messagesPerField.existsError('password')>
                        <span id="input-error-password" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                            ${kcSanitize(messagesPerField.get('password'))?no_esc}
                        </span>
                    </#if>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="password-confirm" class="${properties.kcLabelClass!}">${msg("passwordConfirm")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <input type="password" id="password-confirm" name="password-confirm"
                           class="${properties.kcInputClass!}"
                           autocomplete="new-password"
                           aria-invalid="<#if messagesPerField.existsError('password-confirm')>true</#if>"
                    />

                    <#if messagesPerField.existsError('password-confirm')>
                        <span id="input-error-password-confirm" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                            ${kcSanitize(messagesPerField.get('password-confirm'))?no_esc}
                        </span>
                    </#if>

                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                        <#if isAppInitiatedAction??>
                            <div class="checkbox">
                                <label><input type="checkbox" id="logout-sessions" name="logout-sessions" value="on" checked> ${msg("logoutOtherSessions")}</label>
                            </div>
                        </#if>
                    </div>
                </div>

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <#if isAppInitiatedAction??>
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("doSubmit")}" />
                        <button class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}" type="submit" name="cancel-aia" value="true" />${msg("doCancel")}</button>
                    <#else>
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("doSubmit")}" />
                    </#if>
                </div>
            </div>
        </form>
        <#if policy??>
        <style>
        .grid-column {
            padding: 0 var(--pf-c-alert__title--PaddingRight) 0 var(--pf-c-alert__title--PaddingLeft);
        }
        </style>
        <script>
            ;(function (){
                var msg = {
                    lowerCase: "${msg("invalidPasswordMinLowerCaseCharsMessage")}",
                    upperCase: "${msg("invalidPasswordMinUpperCaseCharsMessage")}",
                    digits: "${msg("invalidPasswordMinDigitsMessage")}",
                    specialChars: "${msg("invalidPasswordMinSpecialCharsMessage")}",
                    length: "${msg("invalidPasswordMinLengthMessage")}",
                    notUsername: "${msg("invalidPasswordNotUsernameMessage")}", 
                    notEmail: "${msg("invalidPasswordNotEmailMessage")}"
                }
                var passwordPolicy = {}
                try {
                    passwordPolicy = JSON.parse('${policy?no_esc}')
                    ;['notUsername', 'notEmail'].forEach(function (key) {
                        passwordPolicy[key] = key in passwordPolicy
                    })
                } catch (e) {}

                // console.log(passwordPolicy)
                var $pn = document.getElementById('password-new')
                var $info = document.getElementById('passwordPolicy')
                var $input = document.querySelector('input[type=submit]')

                $pn.addEventListener('input', function (ev) {
                    $info.innerHTML = null
                    var childs = validate($pn.value).map((info) => 
                        h('div', { className: 'pf-c-alert__title grid-column kc-feedback-text' }, info)
                    )
                    if (childs.length) {
                        $input.disabled = true
                        $info.appendChild(
                            h('div', { className: 'alert-error pf-c-alert pf-m-inline pf-m-danger' }, [
                                h('div', { className: 'pf-c-alert__icon' }, [
                                    h('span', { className: 'fa fa-fw fa-exclamation-circle' }),
                                    h('span', {}, childs)
                                ])
                            ])
                        )
                    } else {
                        $input.disabled = false
                    }
                })

                function h (e, p, c) {
                    var $ = document.createElement(e)
                    Object.entries(p).forEach(([k,v]) => { 
                        k === 'style'
                            ? Object.entries(v).forEach(([p, v]) => { $[k][p] = v })
                            : $[k] = v 
                    })
                    ;[].concat(c).forEach(c => { c && $.append(c) })
                    return $
                }

                var RE = /^((?:[^:,]+)[:,])\s/

                function replace (info) {
                    return info.replace(RE, '')
                }

                function validate (value) {
                    var o = { 
                        lowerCase: 0,
                        upperCase: 0,
                        digits: 0,
                        specialChars: 0,
                        length: value.length
                    }
                    for (var i = 0; i < value.length; i++) {
                        var c = value.charAt(i)
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
                    var info = []
                    Object.keys(o).forEach(function (key) {
                        if (passwordPolicy[key] && passwordPolicy[key] > o[key]) {
                            info.push(replace(msg[key].replace('{0}', passwordPolicy[key])))
                        }
                    })
                    if (passwordPolicy.notUsername && value === '${username}') {
                        info.push(replace(msg.notUsername))
                    }
                    if (info.length) {
                        info.unshift((RE.exec(msg.lowerCase) || [])[1])
                    }
                    return info
                }
            })()
        </script>
        </#if>
    </#if>
</@layout.registrationLayout>