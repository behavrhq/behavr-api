/*
 * Behavr JS SDK
 * Enterprise Behavioral Intelligence SDK
 * Version: 1.0.0
 *
 * Integration:
 *
 * <script>
 *   window.BehavrConfig = {
 *     siteId: "site_123",
 *     debug: true
 *   };
 * </script>
 *
 * <script async src="https://cdn.behavr.io/behavr.js"></script>
 *
 * HTML attributes:
 *
 * Search:
 *   <form data-bh-search-form>
 *     <input data-bh-search-input name="q" />
 *   </form>
 *
 * Product:
 *   <div
 *     data-bh-product
 *     data-bh-product-id="sku_123"
 *     data-bh-product-name="Nike Hoodie"
 *     data-bh-product-category="Hoodies"
 *     data-bh-product-price="79.99"
 *     data-bh-product-currency="USD">
 *   </div>
 *
 * Add to cart:
 *   <button
 *     data-bh-add-to-cart
 *     data-bh-product-id="sku_123">
 *   </button>
 *
 * Checkout:
 *   <button
 *     data-bh-checkout-start
 *     data-bh-cart-total="159.98">
 *   </button>
 *
 * Purchase:
 *   <div
 *     data-bh-purchase
 *     data-bh-order-id="order_1001"
 *     data-bh-order-total="159.98">
 *   </div>
 */

(function (window, document) {
    "use strict";

    const SDK_VERSION = "1.0.0";

    const API_ENDPOINT =
        "https://api.behavr.io/v1/events";

    const STORAGE_PREFIX = "behavr_";

    const state = {
        siteId: null,
        debug: false,

        batchSize: 10,
        flushIntervalMs: 5000,
        maxRetries: 3,

        initialized: false,

        queue: [],
        flushTimer: null,

        observedProductViews: {},

        searchDebounceMs: 700,
        searchInputTimers: {},
        lastTrackedSearchValues: {}
    };

    function log(...args) {
        if (!state.debug) return;
        console.log("[Behavr]", ...args);
    }

    function nowIso() {
        return new Date().toISOString();
    }

    function uuid() {
        if (window.crypto?.randomUUID) {
            return window.crypto.randomUUID();
        }

        return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(
            /[xy]/g,
            function (c) {
                const r = Math.random() * 16 | 0;
                const v = c === "x"
                    ? r
                    : (r & 0x3 | 0x8);

                return v.toString(16);
            }
        );
    }

    function getStorage(key) {
        try {
            return localStorage.getItem(
                STORAGE_PREFIX + key
            );
        } catch {
            return null;
        }
    }

    function setStorage(key, value) {
        try {
            localStorage.setItem(
                STORAGE_PREFIX + key,
                value
            );
        } catch {}
    }

    function getOrCreateAnonymousId() {
        let id = getStorage("anonymous_id");

        if (!id) {
            id = uuid();
            setStorage("anonymous_id", id);
        }

        return id;
    }

    function getOrCreateSessionId() {
        let sessionId = getStorage("session_id");

        const lastSeen = parseInt(
            getStorage("session_last_seen") || "0",
            10
        );

        const now = Date.now();

        const THIRTY_MINUTES =
            30 * 60 * 1000;

        if (
            !sessionId ||
            now - lastSeen > THIRTY_MINUTES
        ) {
            sessionId = uuid();

            setStorage(
                "session_id",
                sessionId
            );
        }

        setStorage(
            "session_last_seen",
            String(now)
        );

        return sessionId;
    }

    function getDeviceType() {
        const width =
            window.innerWidth ||
            document.documentElement.clientWidth;

        if (width < 768) {
            return "mobile";
        }

        if (width < 1024) {
            return "tablet";
        }

        return "desktop";
    }

    function getDatasetValue(el, key) {
        if (!el) return null;

        return el.getAttribute(
            "data-bh-" + key
        );
    }

    function numberOrNull(value) {
        if (
            value === null ||
            value === undefined ||
            value === ""
        ) {
            return null;
        }

        const n = Number(value);

        return isNaN(n)
            ? null
            : n;
    }

    function textOrNull(value) {
        if (
            value === null ||
            value === undefined ||
            value === ""
        ) {
            return null;
        }

        return String(value);
    }

    function getUtmParams() {
        const params =
            new URLSearchParams(
                window.location.search
            );

        const utm = {};

        [
            "utm_source",
            "utm_medium",
            "utm_campaign",
            "utm_term",
            "utm_content"
        ].forEach((key) => {
            const value = params.get(key);

            if (value) {
                utm[key] = value;
            }
        });

        return utm;
    }

    function getBaseEvent(
        eventType,
        properties = {}
    ) {
        return {
            event_id: uuid(),

            event_type: eventType,

            site_id: state.siteId,

            anonymous_id:
                getOrCreateAnonymousId(),

            session_id:
                getOrCreateSessionId(),

            occurred_at: nowIso(),

            url: window.location.href,

            path:
            window.location.pathname,

            title: document.title,

            referrer:
                document.referrer || null,

            user_agent:
            navigator.userAgent,

            browser_language:
                navigator.language || null,

            device_type:
                getDeviceType(),

            sdk_version:
            SDK_VERSION,

            utm: getUtmParams(),

            properties
        };
    }

    function enqueue(event) {
        state.queue.push(event);

        log(
            "Queued event:",
            event.event_type,
            event
        );

        if (
            state.queue.length >=
            state.batchSize
        ) {
            flush();
        }
    }

    async function sendBatch(
        events,
        attempt = 1
    ) {
        if (!events.length) return;

        const payload = {
            site_id: state.siteId,

            sent_at: nowIso(),

            events
        };

        try {
            const response = await fetch(
                API_ENDPOINT,
                {
                    method: "POST",

                    headers: {
                        "Content-Type":
                            "application/json"
                    },

                    body: JSON.stringify(
                        payload
                    ),

                    keepalive: true
                }
            );

            if (!response.ok) {
                throw new Error(
                    `HTTP ${response.status}`
                );
            }

            log(
                "Sent batch:",
                events.length
            );
        } catch (error) {
            log(
                "Send failed:",
                error.message
            );

            if (
                attempt < state.maxRetries
            ) {
                const delay =
                    Math.pow(2, attempt) * 500;

                setTimeout(() => {
                    sendBatch(
                        events,
                        attempt + 1
                    );
                }, delay);
            }
        }
    }

    function flush() {
        if (!state.queue.length) {
            return;
        }

        const events =
            state.queue.splice(
                0,
                state.batchSize
            );

        sendBatch(events);
    }

    function flushAllSync() {
        if (!state.queue.length) {
            return;
        }

        const events =
            state.queue.splice(
                0,
                state.queue.length
            );

        const payload = JSON.stringify({
            site_id: state.siteId,

            sent_at: nowIso(),

            events
        });

        if (navigator.sendBeacon) {
            const blob = new Blob(
                [payload],
                {
                    type:
                        "application/json"
                }
            );

            navigator.sendBeacon(
                API_ENDPOINT,
                blob
            );

            return;
        }

        fetch(API_ENDPOINT, {
            method: "POST",

            headers: {
                "Content-Type":
                    "application/json"
            },

            body: payload,

            keepalive: true
        });
    }

    function track(
        eventType,
        properties = {}
    ) {
        if (!state.initialized) {
            return;
        }

        enqueue(
            getBaseEvent(
                eventType,
                properties
            )
        );
    }

    function trackPageView() {
        track("page_view", {
            source: "auto"
        });
    }

    function readProductData(el) {
        const productRoot =
            el.closest?.(
                "[data-bh-product]"
            );

        function get(key) {
            return (
                getDatasetValue(el, key) ||
                getDatasetValue(
                    productRoot,
                    key
                )
            );
        }

        return {
            product_id:
                textOrNull(
                    get("product-id")
                ),

            product_name:
                textOrNull(
                    get("product-name")
                ),

            category:
                textOrNull(
                    get("product-category")
                ),

            price:
                numberOrNull(
                    get("product-price")
                ),

            currency:
                textOrNull(
                    get("product-currency")
                ),

            quantity:
                numberOrNull(
                    get("quantity")
                ) || 1
        };
    }

    function trackSearch(
        query,
        resultsCount,
        extra = {}
    ) {
        if (!query) return;

        track(
            "search",
            {
                query:
                    String(query).trim(),

                results_count:
                    typeof resultsCount ===
                    "number"
                        ? resultsCount
                        : null,

                ...extra
            }
        );
    }

    function getSearchResultsCount() {
        const el =
            document.querySelector(
                "[data-bh-search-results-count]"
            );

        if (!el) {
            return null;
        }

        return numberOrNull(
            el.getAttribute(
                "data-bh-search-results-count"
            )
        );
    }

    function getSearchInputKey(
        input
    ) {
        return (
            input.getAttribute("id") ||
            input.getAttribute(
                "name"
            ) ||
            "default"
        );
    }

    function bindSearchTracking() {
        document.addEventListener(
            "submit",
            (event) => {
                const form =
                    event.target;

                if (
                    !form?.querySelector
                ) {
                    return;
                }

                const input =
                    form.querySelector(
                        `
            [data-bh-search-input],
            input[type='search'],
            input[name='q'],
            input[name='search']
          `
                    );

                if (
                    !input?.value
                ) {
                    return;
                }

                trackSearch(
                    input.value,
                    getSearchResultsCount(),
                    {
                        source:
                            "form_submit"
                    }
                );
            },
            true
        );

        document.addEventListener(
            "input",
            (event) => {
                const input =
                    event.target;

                if (
                    !input?.matches?.(
                        `
            [data-bh-search-input],
            input[type='search'],
            input[name='q'],
            input[name='search']
          `
                    )
                ) {
                    return;
                }

                const query =
                    input.value.trim();

                if (
                    query.length < 2
                ) {
                    return;
                }

                const key =
                    getSearchInputKey(
                        input
                    );

                if (
                    state.searchInputTimers[
                        key
                        ]
                ) {
                    clearTimeout(
                        state.searchInputTimers[
                            key
                            ]
                    );
                }

                state.searchInputTimers[
                    key
                    ] = setTimeout(() => {

                    if (
                        state.lastTrackedSearchValues[
                            key
                            ] === query
                    ) {
                        return;
                    }

                    state.lastTrackedSearchValues[
                        key
                        ] = query;

                    trackSearch(
                        query,
                        getSearchResultsCount(),
                        {
                            source:
                                "live_input"
                        }
                    );

                }, state.searchDebounceMs);

            },
            true
        );
    }

    function bindClickTracking() {
        document.addEventListener(
            "click",
            (event) => {
                const el =
                    event.target;

                if (!el?.closest) {
                    return;
                }

                const productClickEl =
                    el.closest(
                        `
            [data-bh-product-click],
            [data-bh-product-id]
          `
                    );

                if (
                    productClickEl &&
                    !productClickEl.closest(
                        "[data-bh-add-to-cart]"
                    )
                ) {
                    const data =
                        readProductData(
                            productClickEl
                        );

                    if (
                        data.product_id
                    ) {
                        track(
                            "product_click",
                            {
                                ...data,
                                source:
                                    "auto_click"
                            }
                        );
                    }
                }

                const addToCartEl =
                    el.closest(
                        "[data-bh-add-to-cart]"
                    );

                if (addToCartEl) {
                    const data =
                        readProductData(
                            addToCartEl
                        );

                    if (
                        data.product_id
                    ) {
                        track(
                            "add_to_cart",
                            {
                                ...data,
                                source:
                                    "auto_click"
                            }
                        );
                    }
                }

                const checkoutEl =
                    el.closest(
                        "[data-bh-checkout-start]"
                    );

                if (checkoutEl) {
                    track(
                        "checkout_start",
                        {
                            cart_total:
                                numberOrNull(
                                    getDatasetValue(
                                        checkoutEl,
                                        "cart-total"
                                    )
                                ),

                            currency:
                                textOrNull(
                                    getDatasetValue(
                                        checkoutEl,
                                        "currency"
                                    )
                                ),

                            items_count:
                                numberOrNull(
                                    getDatasetValue(
                                        checkoutEl,
                                        "items-count"
                                    )
                                ),

                            source:
                                "auto_click"
                        }
                    );
                }
            },
            true
        );
    }

    function observeProductViews() {
        const elements =
            document.querySelectorAll(
                "[data-bh-product]"
            );

        if (
            !(
                "IntersectionObserver" in
                window
            )
        ) {
            return;
        }

        const observer =
            new IntersectionObserver(
                (entries) => {
                    entries.forEach(
                        (entry) => {

                            if (
                                !entry.isIntersecting
                            ) {
                                return;
                            }

                            const el =
                                entry.target;

                            const data =
                                readProductData(
                                    el
                                );

                            if (
                                !data.product_id
                            ) {
                                return;
                            }

                            if (
                                state
                                    .observedProductViews[
                                    data.product_id
                                    ]
                            ) {
                                return;
                            }

                            state
                                .observedProductViews[
                                data.product_id
                                ] = true;

                            track(
                                "product_view",
                                {
                                    ...data,
                                    source:
                                        "intersection_observer"
                                }
                            );

                            observer.unobserve(
                                el
                            );
                        }
                    );
                },
                {
                    threshold: 0.5
                }
            );

        elements.forEach(
            (el) => {
                observer.observe(el);
            }
        );
    }

    function trackPurchaseFromDom() {
        const el =
            document.querySelector(
                "[data-bh-purchase]"
            );

        if (!el) {
            return;
        }

        const orderId =
            getDatasetValue(
                el,
                "order-id"
            );

        if (!orderId) {
            return;
        }

        const alreadyTracked =
            getStorage(
                "purchase_" +
                orderId
            );

        if (
            alreadyTracked === "1"
        ) {
            return;
        }

        track(
            "purchase",
            {
                order_id: orderId,

                total:
                    numberOrNull(
                        getDatasetValue(
                            el,
                            "order-total"
                        )
                    ),

                currency:
                    textOrNull(
                        getDatasetValue(
                            el,
                            "currency"
                        )
                    ),

                source: "auto_dom"
            }
        );

        setStorage(
            "purchase_" +
            orderId,
            "1"
        );
    }

    function startFlushTimer() {
        if (
            state.flushTimer
        ) {
            clearInterval(
                state.flushTimer
            );
        }

        state.flushTimer =
            setInterval(
                flush,
                state.flushIntervalMs
            );
    }

    function init(config = {}) {
        if (
            !config.siteId
        ) {
            throw new Error(
                "Behavr siteId is required"
            );
        }

        state.siteId =
            config.siteId;

        state.debug =
            Boolean(config.debug);

        state.initialized = true;

        startFlushTimer();

        bindSearchTracking();

        bindClickTracking();

        trackPageView();

        if (
            document.readyState ===
            "loading"
        ) {
            document.addEventListener(
                "DOMContentLoaded",
                () => {
                    observeProductViews();

                    trackPurchaseFromDom();
                }
            );
        } else {
            observeProductViews();

            trackPurchaseFromDom();
        }

        log(
            "Initialized",
            {
                siteId:
                state.siteId,

                version:
                SDK_VERSION
            }
        );
    }

    window.addEventListener(
        "beforeunload",
        flushAllSync
    );

    document.addEventListener(
        "visibilitychange",
        () => {
            if (
                document.visibilityState ===
                "hidden"
            ) {
                flushAllSync();
            }
        }
    );

    window.Behavr = {
        init,
        track,
        flush,
        version: SDK_VERSION
    };

    if (
        window.BehavrConfig
    ) {
        init(
            window.BehavrConfig
        );
    }

})(window, document);