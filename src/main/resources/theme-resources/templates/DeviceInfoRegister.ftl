<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "title">
        设备认证
    <#elseif section = "header">
        设备信息检测
    <#elseif section = "form">
    <#-- 如果 action 设置了错误消息，显示出来 -->
        <#if message?exists>
            <div class="kc-feedback-text">${message}</div>
        </#if>
        <form id="kc-device-info-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label class="${properties.kcLabelClass!}">我们将检测你的终端设备信息以完成认证</label>
                </div>

                <div class="${properties.kcInputWrapperClass!}">
                    <input type="hidden" id="cpuid" name="cpuid" value=""/>
                    <input type="hidden" id="device_fingerprint" name="device_fingerprint" value=""/>
                    <div id="device-status">正在检测设备信息，请稍候…</div>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <div class="${properties.kcFormButtonsWrapperClass!}">
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                               name="login" id="kc-device-submit" type="submit" value="${msg("doSubmit")}" />
                    </div>
                </div>
            </div>
        </form>
        <noscript>
            <div class="kc-feedback-text">请启用 JavaScript 以继续设备校验。</div>
        </noscript>

        <script>
            (function () {
                var statusEl = document.getElementById('device-status');
                var cpuidEl = document.getElementById('cpuid');
                var fpEl = document.getElementById('device_fingerprint');
                // var rawEl = document.getElementById('device_raw');
                var form = document.getElementById('kc-device-info-form');
                var submitBtn = document.getElementById('kc-device-submit');

                function setError(msg) {
                    statusEl.textContent = msg;
                    submitBtn.disabled = false; // 允许用户手动提交或重试
                }

                // 以下假设你的前端已有一个 getDeviceInfo(callback) 函数，
                // callback 接收一个对象 { cpuid: '...', fingerprint: '...', raw: '...' }
                // 若你已有不同接口，请把这段替换为你的实现。
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
                            statusEl.textContent = '检测到设备信息，请提交';
                            submitBtn.disabled = false;
                            // 自动提交表单触发 Authenticator.action(...)
                            // form.submit();
                        });
                    } catch (e) {
                        console && console.error(e);
                        setError('检测设备信息时发生异常，请重试。');
                    }
                } else {
                    // 如果没有提供 getDeviceInfo，建议把你的 JS 放到此处或更改此逻辑
                    setError('前端未提供 getDeviceInfo() 方法，请将采集脚本集成到此页面后重试。');
                }
            })();

            function getDeviceInfo(callback) {
                callback({"cpuid" : "abcdef", "fingerprint" : "finger"});
            }
        </script>
    </#if>
</@layout.registrationLayout>