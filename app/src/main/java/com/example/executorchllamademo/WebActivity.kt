
package com.example.executorchllamademo

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebViewAssetLoader

class WebActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#FAF8F5"))
        }

        webView = WebView(this)

        container.addView(
            webView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        setContentView(container)

        ViewCompat.setOnApplyWindowInsetsListener(container) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )

            view.setPadding(
                insets.left,
                insets.top,
                insets.right,
                insets.bottom
            )

            WindowInsetsCompat.CONSUMED
        }

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler(
                "/assets/",
                WebViewAssetLoader.AssetsPathHandler(this)
            )
            .build()

        webView.webViewClient = object : WebViewClient() {

            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? =
                assetLoader.shouldInterceptRequest(request.url)

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                val uiPatchJs = """
                    (function() {

                        const langMap = [
                            { marker: 'తెలుగు', name: 'అలెక్స్ మోర్గాన్', grade: '11వ తరగతి / ఇంటర్మీడియట్', ref: 'మూల ఆధారాలు:' },
                            { marker: 'हिन्दी', name: 'अलेक्स मॉर्गन', grade: 'कक्षा 11 / 12', ref: 'स्रोत संदर्भ:' },
                            { marker: 'हिंदी', name: 'अलेक्स मॉर्गन', grade: 'कक्षा 11 / 12', ref: 'स्रोत संदर्भ:' },
                            { marker: 'தமிழ்', name: 'அலெக்ஸ் மோர்கன்', grade: 'வகுப்பு 11 / 12', ref: 'மூல ஆதாரங்கள்:' },
                            { marker: 'ಕನ್ನಡ', name: 'ಅಲೆಕ್ಸ್ ಮೋರ್ಗನ್', grade: 'ತರಗತಿ 11 / 12', ref: 'ಮೂಲ ಉಲ್ಲೇಖಗಳು:' },
                            { marker: 'മലയാളം', name: 'അലക്സ് മോർഗൻ', grade: 'ക്ലാസ് 11 / 12', ref: 'ഉറവിട റഫറൻസുകൾ:' },
                            { marker: 'বাংলা', name: 'অ্যালেক্স মরগান', grade: 'শ্রেণি ১১ / ১২', ref: 'উৎস তথ্যসূত্র:' },
                            { marker: 'मराठी', name: 'अलेक्स मॉर्गन', grade: 'इयत्ता ११ / १२', ref: 'स्रोत संदर्भ:' },
                            { marker: 'ગુજરાતી', name: 'એલેક્સ મોર્ગન', grade: 'ધોરણ ૧૧ / ૧૨', ref: 'સ્રોત સંદર્ભો:' },
                            { marker: 'ਪੰਜਾਬੀ', name: 'ਅਲੈਕਸ ਮੋਰਗਨ', grade: 'ਜਮਾਤ 11 / 12', ref: 'ਸਰੋਤ ਹਵਾਲੇ:' },
                            { marker: 'ଓଡ଼ିଆ', name: 'ଆଲେକ୍ସ ମୋର୍ଗାନ୍', grade: 'ଶ୍ରେଣୀ ୧୧ / ୧୨', ref: 'ଉତ୍ସ ସନ୍ଦର୍ଭ:' },
                            { marker: 'অসমীয়া', name: 'এলেক্স মৰ্গান', grade: 'শ্ৰেণী ১১ / ১২', ref: 'উৎস প্ৰসংগ:' },
                            { marker: 'اردو', name: 'ایلکس مورگن', grade: 'جماعت 11 / 12', ref: 'حوالہ جات:' },
                            { marker: 'कॉशुर', name: 'एलक्स मॉर्गन', grade: 'جماعت 11 / 12', ref: 'حوالہ جات:' },
                            { marker: 'كٲشُر', name: 'ایلکس مورگن', grade: 'جماعت 11 / 12', ref: 'حوالہ جات:' },
                            { marker: 'कोंकणी', name: 'अलेक्स मॉर्गन', grade: 'इयत्ता ११ / १२', ref: 'स्रोत संदर्भ:' },
                            { marker: 'मैथिली', name: 'अलेक्स मॉर्गन', grade: 'वर्ग ११ / १२', ref: 'स्रोत संदर्भ:' },
                            { marker: 'डोगरी', name: 'अलेक्स मॉर्गन', grade: 'जमात 11 / 12', ref: 'स्रोत संदर्भ:' },
                            { marker: 'नेपाली', name: 'एलेक्स मोर्गन', grade: 'कक्षा ११ / १२', ref: 'स्रोत सन्दर्भहरू:' },
                            { marker: 'संस्कृतम्', name: 'अलेक्स् मोर्गन्', grade: 'कक्षा एकादशी / द्वादशी', ref: 'मूलसन्दर्भाः:' },
                            { marker: 'سنڌي', name: 'ايلڪس مورگن', grade: 'ڪلاس 11 / 12', ref: 'ذريعا:' },
                            { marker: 'सिन्धी', name: 'अलेक्स मॉर्गन', grade: 'दर्जो 11 / 12', ref: 'स्रोत संदर्भ:' },
                            { marker: 'ꯃꯩꯇꯩꯂꯣꯟ', name: 'ꯑꯂꯦꯛꯁ ꯃꯣꯔꯒꯟ', grade: 'ꯀ꯭ꯂꯥꯁ ꯱꯱ / ꯱꯲', ref: 'ꯍꯧꯔꯛꯐꯝ:' },
                            { marker: 'মৈতৈলোন্', name: 'অ্যালেক্স মরগান', grade: 'ক্লাস ১১ / ১২', ref: 'উৎস:' },
                            { marker: 'बड़ो', name: 'एलेक्स मर्गान', grade: 'थाखो 11 / 12', ref: 'फुंखा:' },
                            { marker: 'ᱥᱟᱱᱛᱟᱲᱤ', name: 'ᱟᱞᱮᱠᱥ ᱢᱚᱨᱜᱟᱱ', grade: 'ᱪᱟᱱᱚᱪ ᱑᱑ / ᱑᱒', ref: 'ᱥᱟᱹᱠᱷᱭᱟᱹᱛ:' }
                        ];

                        const refHeaders = [
                            'SOURCE REFERENCES:',
                            ...langMap.map(l => l.ref)
                        ];

                        function injectCustomQuizCard() {

                            const bodyText = document.body.innerText || '';

                            const onQuizzesTab =
                                bodyText.includes('Recommended Next Steps') ||
                                bodyText.includes('Quizzes & Topic Mastery');

                            const existing = document.getElementById('sylva-custom-quiz-box');

                            if (!onQuizzesTab) {
                                if (existing) existing.remove();
                                return;
                            }

                            if (existing) return;

                            const heading = Array.from(
                                document.querySelectorAll('h1, h2, h3, h4')
                            ).find(
                                el => el.textContent &&
                                    el.textContent.includes('Recommended Next Steps')
                            );

                            if (!heading) return;

                            let targetContainer = heading.parentElement;

                            while (
                                targetContainer &&
                                targetContainer.parentElement &&
                                targetContainer.parentElement.tagName !== 'MAIN' &&
                                targetContainer.parentElement.children.length < 3
                            ) {
                                targetContainer = targetContainer.parentElement;
                            }

                            if (!targetContainer || !targetContainer.parentElement) return;

                            const box = document.createElement('div');
                            box.id = 'sylva-custom-quiz-box';

                            box.style.cssText =
                                'background:#FFFFFF;border:1.5px solid #1B4332;border-radius:16px;padding:16px;margin:12px 0 18px 0;box-shadow:0 3px 10px rgba(27,67,50,0.08);';

                            const badge = document.createElement('div');
                            badge.style.cssText =
                                'font-size:11px;font-weight:700;letter-spacing:0.6px;color:#1B4332;background:#E8F3EE;display:inline-block;padding:4px 10px;border-radius:999px;margin-bottom:8px;';

                            badge.textContent = 'OFFLINE AI QUIZ GENERATOR (10 MCQs)';

                            const title = document.createElement('div');
                            title.style.cssText =
                                'font-size:17px;font-weight:700;color:#1C1917;margin-bottom:4px;';

                            title.textContent = 'Generate Quiz on Any Topic';

                            const sub = document.createElement('div');
                            sub.style.cssText =
                                'font-size:13px;color:#57534E;margin-bottom:12px;';

                            sub.textContent =
                                'Type any subject or concept (e.g. Organic Chemistry, Ohms Law, Thermodynamics). Generates 10 interactive MCQs and syncs your score to your Study Plan.';

                            const row = document.createElement('div');
                            row.style.cssText = 'display:flex;gap:8px;margin-bottom:6px;';

                            const input = document.createElement('input');
                            input.id = 'sylva-topic-input';
                            input.type = 'text';
                            input.placeholder = 'Enter any topic...';

                            input.style.cssText =
                                'flex:1;padding:10px 12px;border:1px solid #D6D3D1;border-radius:10px;font-size:14px;background:#FAF8F5;color:#1C1917;';

                            const btn = document.createElement('button');
                            btn.id = 'sylva-gen-quiz-btn';
                            btn.type = 'button';
                            btn.textContent = 'Generate 10';

                            btn.style.cssText =
                                'padding:10px 14px;background:#1B4332;color:#FFFFFF;border:none;border-radius:10px;font-weight:600;font-size:13px;';

                            const area = document.createElement('div');
                            area.id = 'sylva-quiz-area';

                            row.appendChild(input);
                            row.appendChild(btn);

                            box.appendChild(badge);
                            box.appendChild(title);
                            box.appendChild(sub);
                            box.appendChild(row);
                            box.appendChild(area);

                            targetContainer.parentElement.insertBefore(box, targetContainer);

                            btn.onclick = () => {

                                const topic = (input.value || '').trim();

                                if (!topic) {
                                    input.focus();
                                    return;
                                }

                                if (!window.AndroidLLM || !window.AndroidLLM.generateTopicQuiz) {
                                    area.textContent = 'Offline quiz engine not ready.';
                                    return;
                                }

                                btn.disabled = true;
                                btn.textContent = 'Generating...';

                                area.innerHTML = '';

                                const st = document.createElement('div');
                                st.id = 'sylva-status-msg';
                                st.style.cssText =
                                    'color:#57534E;font-size:13px;margin-top:8px;';

                                st.textContent =
                                    'Generating Questions 1–5 on ' + topic + '...';

                                const banner = document.createElement('div');
                                banner.id = 'sylva-score-banner';
                                banner.style.cssText =
                                    'font-size:13px;font-weight:600;color:#1B4332;margin:8px 0 4px;';

                                const list = document.createElement('div');
                                list.id = 'sylva-cards-list';

                                area.appendChild(st);
                                area.appendChild(banner);
                                area.appendChild(list);

                                let totalQuestions = 0;
                                let answered = 0;
                                let correctCount = 0;

                                window.onQuizError = (err) => {
                                    btn.disabled = false;
                                    btn.textContent = 'Generate 10';

                                    st.style.color = '#B91C1C';
                                    st.textContent = err;
                                };

                                window.onQuizGenerated = (genTopic, batchNum, mcqs, rawFallback) => {

                                    if ((!mcqs || !mcqs.length) && totalQuestions === 0) {
                                        btn.disabled = false;
                                        btn.textContent = 'Generate 10';

                                        st.style.whiteSpace = 'pre-wrap';
                                        st.textContent = rawFallback;
                                        return;
                                    }

                                    mcqs.forEach((item) => {

                                        totalQuestions++;

                                        const qNumber = totalQuestions;
                                        const card = document.createElement('div');

                                        card.style.cssText =
                                            'background:#FAF8F5;border:1px solid #E7E5E4;border-radius:12px;padding:12px;margin-top:10px;';

                                        const qTitle = document.createElement('div');

                                        qTitle.style.cssText =
                                            'font-weight:600;font-size:14px;color:#1C1917;margin-bottom:8px;';

                                        qTitle.textContent = qNumber + '. ' + item.q;

                                        card.appendChild(qTitle);

                                        const feedback = document.createElement('div');

                                        feedback.style.cssText =
                                            'font-size:12px;margin-top:8px;display:none;';

                                        let locked = false;

                                        ['A', 'B', 'C', 'D'].forEach(letter => {

                                            const optText = item[letter];

                                            if (!optText) return;

                                            const optBtn = document.createElement('button');

                                            optBtn.type = 'button';
                                            optBtn.textContent = letter + ') ' + optText;

                                            optBtn.style.cssText =
                                                'display:block;width:100%;text-align:left;padding:8px 10px;margin-top:6px;border-radius:8px;border:1px solid #D6D3D1;background:#FFFFFF;color:#1C1917;font-size:13px;';

                                            optBtn.onclick = () => {

                                                if (locked) return;

                                                locked = true;
                                                answered++;

                                                const isRight = letter === item.ans;

                                                const expText = item.exp ? (' ' + item.exp) : '';

                                                if (isRight) {
                                                    correctCount++;

                                                    optBtn.style.background = '#DCFCE7';
                                                    optBtn.style.borderColor = '#16A34A';

                                                    feedback.style.color = '#15803D';
                                                    feedback.textContent = 'Correct!' + expText;

                                                } else {
                                                    optBtn.style.background = '#FEE2E2';
                                                    optBtn.style.borderColor = '#DC2626';

                                                    feedback.style.color = '#B91C1C';

                                                    feedback.textContent =
                                                        'Incorrect (Answer: ' + item.ans + ').' + expText;
                                                }

                                                feedback.style.display = 'block';

                                                window.AndroidLLM && window.AndroidLLM.recordAnswer(genTopic, isRight);

                                                banner.textContent =
                                                    'Topic: ' + genTopic +
                                                    ' — Score: ' + correctCount + '/' + totalQuestions +
                                                    (answered === totalQuestions ? ' (Saved to Study Planner!)' : '');
                                            };

                                            card.appendChild(optBtn);
                                        });

                                        card.appendChild(feedback);
                                        list.appendChild(card);
                                    });

                                    banner.textContent =
                                        'Topic: ' + genTopic +
                                        ' — Score: ' + correctCount + '/' + totalQuestions;

                                    if (batchNum === 1 && totalQuestions < 10) {

                                        st.textContent =
                                            'Generating Questions 6–10 on ' + genTopic +
                                            '... (You can start answering 1–5 now!)';

                                        window.AndroidLLM.generateTopicQuiz(genTopic, 2);

                                    } else {

                                        btn.disabled = false;
                                        btn.textContent = 'Generate 10';

                                        st.remove();
                                    }
                                };

                                window.AndroidLLM.generateTopicQuiz(topic, 1);
                            };
                        }

                        const patchUi = () => {

                            try {

                                const bodyText = document.body.innerText || '';

                                const active = langMap.find(l => bodyText.includes(l.marker));

                                document.querySelectorAll('div, section').forEach(el => {

                                    if (el.children.length > 0 && el.innerText) {

                                        const txt = el.innerText.trim();

                                        if (refHeaders.some(h => txt.startsWith(h))) {
                                            el.style.display = 'none';
                                        }
                                    }
                                });

                                injectCustomQuizCard();

                                if (!active) return;

                                const walker = document.createTreeWalker(
                                    document.body,
                                    NodeFilter.SHOW_TEXT
                                );

                                let node;

                                while ((node = walker.nextNode())) {

                                    if (node.nodeValue.includes('Alex Morgan')) {
                                        node.nodeValue = node.nodeValue.replace('Alex Morgan', active.name);
                                    }

                                    if (node.nodeValue.includes('Grade 11 / Year 12')) {
                                        node.nodeValue = node.nodeValue.replace(
                                            'Grade 11 / Year 12',
                                            active.grade
                                        );
                                    }
                                }

                            } catch (e) {
                                console.error(e);
                            }
                        };

                        patchUi();

                        new MutationObserver(patchUi).observe(document.body, {
                            childList: true,
                            subtree: true,
                            characterData: true
                        });

                    })();
                """.trimIndent()

                view?.evaluateJavascript(uiPatchJs, null)
            }
        }

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.addJavascriptInterface(
            LlmBridge(this, webView),
            "AndroidLLM"
        )

        val host = "appassets" + ".androidplatform.net"

        webView.loadUrl("https://$host/assets/web/index.html")
    }

    override fun onDestroy() {
        webView.removeJavascriptInterface("AndroidLLM")
        super.onDestroy()
    }
}
