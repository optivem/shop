# Business Rules: Browse Coupons

Use case: [UC-005 Browse Coupons](use-case.md). Acceptance criteria: [acceptance-criteria.md](acceptance-criteria.md).

- BR-1. The coupon list shows every published coupon, newest published first, so a freshly published coupon is always at the top of the first page.
- BR-2. Each coupon shows its code, discount rate as a percentage with 2 decimals (e.g. 15.00%), valid-from time ("Immediate" when there is none), valid-to time ("Never" when there is none), usage limit ("Unlimited" when there is none) and used count. Times are shown in UTC.
- BR-3. The list is read one page at a time. Pages are numbered from 1. Without a page the first page is shown; without a page size, 50 coupons per page. The page size is at most 100. The result reports the total number of coupons. A page past the last one is empty, not an error.
- BR-4. The used count is the number of orders that have used the coupon (UC-001 Place Order adds one per order).
