<#import "template.ftl" as layout>
	<@layout.registrationLayout displayInfo=true displayRequiredFields=false; section>
		<style>
			#kc-username {
				display: none;
			}
			.text-center {
				text-align: center;
			}
		</style>
		<#if section="header">

		<#elseif section="title">

		<#elseif section="form">
			<h1 class="text-center">${msg("authenticatorCode")}</h1>

			<form action="${url.loginAction}" class="${properties.kcFormClass!}" id="kc-u2f-login-form" method="post">
				<div class="${properties.kcFormGroupClass!}">
					<div class="${properties.kcLabelWrapperClass!}">
						<label for="otp" class="${properties.kcLabelClass!}">${msg("loginOtpOneTime")}</label>
					</div>

					<div class="${properties.kcInputWrapperClass!}">
						<input id="otp" name="challenge_input" autocomplete="one-time-code" inputmode="numeric" maxlength="10"
							type="text" required spellcheck="false" placeholder="${msg("mfaOtpEnterCodePlaceholder")}"
							class="${properties.kcInputClass!}" autofocus />
					</div>
				</div>

				<input
					class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"
					type="submit" value="${msg("doSubmit")}" />

				<input class="${properties.kcButtonClass!} pf-m-secondary ${properties.kcButtonLargeClass!}" type="submit"
					name="cancel" value="${msg("doCancel")}" />

			</form>
			<script>
				;(function () {
					if ('OTPCredential' in window) {
						const $input = document.querySelector('input[autocomplete="one-time-code"]');
						if (!$input) return;
						const ac = new AbortController();
						const $form = $input.closest('form');
						if ($form) {
							$form.addEventListener('submit', e => {
								ac.abort();
							});
						}
						navigator.credentials.get({
							otp: { transport: ['sms'] },
							signal: ac.signal
						}).then(otp => {
							$input.value = otp.code;
							if ($form) $form.submit();
						}).catch(err => {
							console.log(err);
						});
					}
				})()
			</script>
		<#elseif section="info">
			<form action="${url.loginAction}" class="${properties.kcFormClass!}" id="kc-u2f-login-form" method="post">
				<p class="instruction">${msg("mfaOtpResendCodeAsk")}</p>
				<input
					class="${properties.kcButtonClass!} pf-m-secondary ${properties.kcButtonLargeClass!}"
					type="submit" name="resend" value="${msg("mfaOtpResendCode")}" />
			</form>
			<script>
			;(function () {
				var $resend = document.getElementById('kc-info')
				if ($resend) {
					$resend.style.display = 'none'
					setTimeout(function () {
						$resend.style.display = 'block'
					}, 5000)
				}
			})()
			</script>
		</#if>
	</@layout.registrationLayout>