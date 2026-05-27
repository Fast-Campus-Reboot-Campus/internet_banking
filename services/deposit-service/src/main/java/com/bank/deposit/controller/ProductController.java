// ── 공통 상품 ──────────────────────────────────────────────────────────────

    @GetMapping("/products")
    public List<Product> list(
            @RequestParam(required = false) ProductType productType,
            @RequestParam(required = false) ProductStatus productStatus) {
        return productService.findAll(productType, productStatus);
    }

    @PostMapping("/products")
    public ResponseEntity<Product> create(@Valid @RequestBody ProductCreateRequest req) {
        Product product = productService.create(
                req.productType(), req.productName(), req.description(),
                req.departmentId(), req.baseInterestRate(),
                req.minPeriodMonth(), req.maxPeriodMonth(),
                req.minJoinAmount(), req.maxJoinAmount(),
                req.isEarlyTerminationAllowed(), req.isTaxBenefitAvailable(),
                req.isAutoRenewalAvailable(), req.releasedAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    @GetMapping("/products/{productId:\\d+}")
    public Product get(@PathVariable Long productId) {
        return productService.findById(productId);
    }

    @PutMapping("/products/{productId}")
    public Product update(@PathVariable Long productId, @Valid @RequestBody ProductUpdateRequest req) {
        return productService.update(productId, req.productName(), req.description(), req.baseInterestRate());
    }