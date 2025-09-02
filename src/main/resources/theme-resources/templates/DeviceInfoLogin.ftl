<#import "template.ftl" as layout>
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

            <#-- 新增：凭证选择区域 -->
            <#if credentials?has_content>
                <div class="${properties.kcFormGroupClass!}">
                    <div class="${properties.kcLabelWrapperClass!}">
                        <div class="${properties.kcFormGroupLabelClass!} ${properties.kcFormLabelClass!}">
                            <label class="${properties.kcFormLabelTextClass!}">请选择认证设备</label>
                        </div>
                    </div>
                    <div class="kc-device-credentials-list" style="margin: 12px 0;">
                        <#list credentials as credential>
                            <div class="kc-device-credential-item"
                                 style="padding: 8px 12px; margin-bottom: 8px; border: 1px solid #e0e0e0; border-radius: 6px; display: flex; align-items: center;">
                                <input type="radio" name="credentialId" id="cred-${credential.id}"
                                       value="${credential.id}"
                                       <#if credential?is_first>checked</#if>
                                       style="margin-right: 10px;"/>
                                <label for="cred-${credential.id}" style="flex: 1; cursor: pointer;">
                                    <strong>${credential.userLabel!credential.type!}</strong>
                                    <#if credential.createdDate??>
                                        <span style="color: #888; font-size: 0.95em; margin-left: 8px;">
                                            (${credential.createdDate?number_to_date?string("yyyy-MM-dd HH:mm")})
                                        </span>
                                    </#if>
                                    <#if credential.type?? && credential.type != "password">
                                        <span style="color: #2196f3; font-size: 0.95em; margin-left: 8px;">
                                            [${credential.type}]
                                        </span>
                                    </#if>
                                </label>
                            </div>
                        </#list>
                    </div>
                </div>
            </#if>

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

            <#-- 设备信息记录开关 -->
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <div class="${properties.kcFormGroupLabelClass!} ${properties.kcFormLabelClass!}">
                        <label class="${properties.kcFormLabelTextClass!}" for="recordDeviceInfoSwitch">
                            是否记录本次设备信息
                        </label>
                    </div>
                </div>
                <div>
                    <label class="pf-v5-c-switch" for="recordDeviceInfoSwitch"
                           data-ouia-component-type="PF5/Switch"
                           data-ouia-safe="true"
                           data-ouia-component-id="OUIA-Generated-Switch-DeviceInfo">
                        <input id="recordDeviceInfoSwitch"
                               class="pf-v5-c-switch__input"
                               type="checkbox"
                               name="recordDeviceInfo"
                               value="true"
                               aria-labelledby="recordDeviceInfoSwitch-off"
                               data-testid="recordDeviceInfoSwitch"
                               aria-label="">
                        <span class="pf-v5-c-switch__toggle"></span>
                        <span class="pf-v5-c-switch__label pf-m-on"
                              id="recordDeviceInfoSwitch-on"
                              aria-hidden="true">开</span>
                        <span class="pf-v5-c-switch__label pf-m-off"
                              id="recordDeviceInfoSwitch-off"
                              aria-hidden="true">关</span>
                    </label>
                </div>
            </div>

            <div id="device-status">正在检测设备信息，请稍候…</div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <div class="${properties.kcFormButtonsWrapperClass!}">
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                               name="login" id="kc-device-submit" type="submit" value="${msg("doLogIn")}"/>
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
                var form = document.getElementById('kc-device-info-form');
                var submitBtn = document.getElementById('kc-device-submit');

                function setError(msg) {
                    statusEl.textContent = msg;
                    submitBtn.disabled = false; // 允许用户手动提交或重试
                }

                // 假设前端已有一个 getDeviceInfo(callback) 函数
                // callback 接收一个对象 { cpuid: '...', fingerprint: '...'}
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
                            statusEl.textContent = '检测到设备信息，请提交以尝试登录';
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
                    .then(data => data.cpuid)
                    .catch(() => {
                        alert("未检测到本地硬件信息服务，请下载安装本地服务。");
                        return ''; // 出错时返回空字符串
                    });

                // 等待两个都完成
                Promise.all([cpuPromise, fpPromise]).then(function ([cpuId, fingerPrint]) {
                    callback({"cpuid": cpuId, "fingerprint": fingerPrint});
                });
            }
        </script>
    </#if>
</@layout.registrationLayout>