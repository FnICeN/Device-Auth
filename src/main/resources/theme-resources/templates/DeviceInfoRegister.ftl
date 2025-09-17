<#import "template.ftl" as layout>
<#import "field.ftl" as field>
<@layout.registrationLayout; section>
    <#if section = "title">
        设备认证
    <#elseif section = "header">
        设备信息检测
    <#elseif section = "form">
        <form id="kc-device-info-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label class="${properties.kcLabelClass!}">我们将检测你的终端设备信息以完成认证</label>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcInputGroup!}">
                    <div class="${properties.kcInputGroupItemClass!} ${properties.kcFill!}">
                        <div style="width: 450px">
                            <@field.input name="host_name" label="主机名称" required=true error=kcSanitize(messagesPerField.getFirstError('host_name'))?no_esc value='' />
                        </div>
                    </div>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <div class="${properties.kcFormGroupLabelClass!} ${properties.kcFormLabelClass!}">
                        <label class="${properties.kcFormLabelTextClass!}">CPU ID</label>
                    </div>
                </div>
                <div class="${properties.kcInputGroup!}">
                    <div class="${properties.kcInputGroupItemClass!} ${properties.kcFill!}">
                        <div class="${properties.kcInputClass!} ${properties.kcFormReadOnlyClass!}">
                            <input id="cpuid" name="cpuid" value="" type="password" readonly/>
                        </div>
                    </div>
                    <div class="${properties.kcInputGroupItemClass!}">
                        <button class="${properties.kcFormPasswordVisibilityButtonClass!}" type="button"
                                aria-label="${msg('showPassword')}"
                                aria-controls="cpuid" data-password-toggle
                                data-icon-show="fa-eye fas" data-icon-hide="fa-eye-slash fas"
                                data-label-show="${msg('showPassword')}" data-label-hide="${msg('hidePassword')}">
                            <i class="fa-eye fas" aria-hidden="true"></i>
                        </button>
                    </div>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <div class="${properties.kcFormGroupLabelClass!} ${properties.kcFormLabelClass!}">
                        <label class="${properties.kcFormLabelTextClass!}">Finger Print</label>
                    </div>
                </div>
                <div class="${properties.kcInputGroup!}">
                    <div class="${properties.kcInputGroupItemClass!} ${properties.kcFill!}">
                        <div class="${properties.kcInputClass!} ${properties.kcFormReadOnlyClass!}">
                            <input id="device_fingerprint" name="device_fingerprint" value="" type="password" readonly/>
                        </div>
                    </div>
                    <div class="${properties.kcInputGroupItemClass!}">
                        <button class="${properties.kcFormPasswordVisibilityButtonClass!}" type="button"
                                aria-label="${msg('showPassword')}"
                                aria-controls="device_fingerprint" data-password-toggle
                                data-icon-show="fa-eye fas" data-icon-hide="fa-eye-slash fas"
                                data-label-show="${msg('showPassword')}" data-label-hide="${msg('hidePassword')}">
                            <i class="fa-eye fas" aria-hidden="true"></i>
                        </button>
                    </div>
                </div>
            </div>

            <div id="device-status">正在检测设备信息，请稍候…</div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <div class="${properties.kcFormButtonsWrapperClass!}">
                        <input type="hidden" id="public_key" name="public_key" value=""/>
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                               name="login" id="kc-device-submit" type="submit" value="${msg("doSubmit")}"/>
                    </div>
                </div>
            </div>
        </form>
        <noscript>
            <div class="kc-feedback-text">请启用 JavaScript 以继续设备校验。</div>
        </noscript>

        <script src="https://openfpcdn.io/fingerprintjs/v3.4.1/umd.min.js"></script>
        <script>
            (function () {
                var statusEl = document.getElementById('device-status');
                var cpuidEl = document.getElementById('cpuid');
                var fpEl = document.getElementById('device_fingerprint');
                var pkEl = document.getElementById('public_key');
                var form = document.getElementById('kc-device-info-form');
                var submitBtn = document.getElementById('kc-device-submit');

                function setError(msg) {
                    statusEl.textContent = msg;
                    submitBtn.disabled = false; // 允许用户手动提交或重试
                }

                // 假设前端已有一个 getDeviceInfo(callback) 函数
                // callback 接收一个对象 { cpuid: '...', fingerprint: '...', raw: '...' }
                if (typeof getDeviceInfo === 'function') {
                    try {
                        // 禁用提交按钮，等待自动提交
                        // submitBtn.disabled = true;
                        getDeviceInfo(function (info) {
                            if (!info) {
                                setError('未能检测到设备信息，请重试。');
                                return;
                            }
                            cpuidEl.value = info.cpuid || '';
                            fpEl.value = info.fingerprint || '';
                            pkEl.value = info.publicKeyJson || '';
                            statusEl.textContent = '检测到设备信息，请提交以进行首台设备的注册';
                            submitBtn.disabled = false;
                            // 自动提交表单触发 Authenticator.action(...)
                            // form.submit();
                        });
                    } catch (e) {
                        console && console.error(e);
                        setError('检测设备信息时发生异常，请重试。');
                    }
                } else {
                    setError('前端未提供 getDeviceInfo() 方法，请将采集脚本集成到此页面后重试。');
                }
            })();

            function getDeviceInfo(callback) {
                // FingerprintJS 异步
                var fpPromise = FingerprintJS.load()
                    .then(fp => fp.get())
                    .then(result => result.visitorId);

                // fetch 异步
                var cpuPromise = fetch("http://127.0.0.1:12345/get_cpuid")
                    .then(response => response.json())
                    .catch(() => {
                        alert("未检测到本地硬件信息服务，请下载安装本地服务。");
                        return ''; // 出错时返回空字符串
                    });

                // 等待两个都完成
                Promise.all([cpuPromise, fpPromise]).then(function ([agentInfo, fingerPrint]) {
                    callback({"cpuid": agentInfo.cpuid, "fingerprint": fingerPrint, "publicKeyJson": agentInfo.publicKeyJson});
                });
            }
        </script>
    </#if>
</@layout.registrationLayout>