<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=false displayRequiredFields=false; section>
    <#if section = "header">

    <#elseif section = "title">

    <#elseif section = "form">
        <style>
            #kc-username {
                display: none;
            }
            #resend {
                padding-top: 3em;
            }
            .text-center {
                text-align: center;
            }
        </style>

        <h1 class="text-center">${msg("authenticatorCode")}</h1>

        <#--  <ul>  -->
            <#--  <#list .data_model?keys as key>  -->
                <#--  <li>${key}</li>  -->
            <#--  </#list>  -->
        <#--  </ul>  -->

        <form action="${url.loginAction}" class="${properties.kcFormClass!}" id="kc-u2f-login-form" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="otp" class="${properties.kcLabelClass!}">${msg("loginOtpOneTime")}</label>
                </div>

                <div class="${properties.kcInputWrapperClass!}">
                    <input id="otp" name="challenge_input" autocomplete="off" inputmode="numeric" type="text"
                           spellcheck="false"
                           placeholder="${msg("mfaOtpEnterCodePlaceholder")}"
                           class="${properties.kcInputClass!}"
                           autofocus/>
                </div>
            </div>

            <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"
                   type="submit" value="${msg("doSubmit")}"/>

            <input class="${properties.kcButtonClass!} pf-m-secondary ${properties.kcButtonLargeClass!}"
                   type="submit" name="cancel" value="${msg("doCancel")}"/>


            <div id="resend">
                <p>${msg("mfaOtpResendCodeAsk")}</p>

                <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"
                        type="submit" name="resend" value="${msg("mfaOtpResendCode")}"/>
            </div>
        </form>
        <script>
            (function(){
                var $resend = document.getElementById('resend')
                $resend.style.display = 'none'
                setTimeout(function () {
                    $resend.style.display = 'block'
                }, 3000)
            })()
        </script>
    </#if>
</@layout.registrationLayout>
