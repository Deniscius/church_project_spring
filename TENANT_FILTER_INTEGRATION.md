// ============================================
// INTEGRATION NOTES - SecurityConfig
// ============================================
// 
// Pour intégrer le TenantFilter à la chaîne de filtres Spring Security,
// ajoutez les dépendances et la configuration suivante :

// 1. Dans SecurityConfig.java, ajouter au Bean securityFilterChain :

/*
@Bean
public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        CorsConfigurationSource corsConfigurationSource,
        DaoAuthenticationProvider authenticationProvider,
        AuthTokenFilter authTokenFilter,
        TenantFilter tenantFilter) throws Exception {  // ← Ajouter le paramètre
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .csrf(AbstractHttpConfigurer::disable)
        .exceptionHandling(ex -> ex.authenticationEntryPoint(authEntryPointJwt))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        
        // ← Ajouter le TenantFilter AVANT le AuthTokenFilter
        .addFilterBefore(tenantFilter, AuthTokenFilter.class)
        
        .authorizeHttpRequests(auth -> auth
            // ... reste de la configuration ...
        )
        .authenticationProvider(authenticationProvider)
        .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);
    
    return http.build();
}
*/

// 2. Alternative : Enregistrer le TenantFilter comme Filter Bean

/*
@Bean
public FilterRegistrationBean<TenantFilter> tenantFilterRegistration(TenantFilter tenantFilter) {
    FilterRegistrationBean<TenantFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(tenantFilter);
    registration.addUrlPatterns("/*");
    registration.setOrder(1);  // Exécuter en premier
    return registration;
}
*/

// ============================================
// UTILISATION - Exemple Complet
// ============================================

// 1. Dans un controller :

@RestController
@RequestMapping("/demandes")
@RequiredArgsConstructor
public class DemandeController {

    @GetMapping
    public ResponseEntity<List<Demande>> getAll() {
        // Le TenantContext contient automatiquement le tenant ID du header
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(demandeService.getByTenant(tenantId));
    }
}

// 2. Dans un service :

@Service
@RequiredArgsConstructor
public class DemandeService {

    public List<Demande> getByTenant(UUID tenantId) {
        return TenantContext.withTenant(tenantId, () -> {
            // Toutes les opérations dans ce bloc connaissent le tenant
            return repository.findAll();
        });
    }
}

// 3. Depuis le frontend (React/JavaScript) :

async function getDemandesByParoisse(paroisseId) {
    const headers = {
        'X-Tenant-ID': paroisseId,  // ← Passer le tenant ID
        'Authorization': `Bearer ${token}`,
    };

    const response = await fetch('http://localhost:8081/demandes', {
        method: 'GET',
        headers,
    });

    return response.json();
}

// ============================================
// TROUBLESHOOTING
// ============================================

// Problème: TenantContext retourne null
// Solution: 
// 1. Vérifier que le header X-Tenant-ID est bien envoyé
// 2. Vérifier que le TenantFilter est bien enregistré dans la chaîne
// 3. Vérifier le format de l'UUID (doit être valide)

// Problème: UUID invalide dans le header
// Log attendu: "Invalid tenant ID format in header: xxx"
// Solution: Envoyer un UUID valide au format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx

// Problème: TenantContext vide après la requête
// C'est normal! Le clear() est appelé dans le finally du filtre
// pour nettoyer le ThreadLocal

// ============================================
// MULTI-DATABASE SUPPORT (Optionnel)
// ============================================

// Si vous avez plusieurs bases de données par tenant :

@Component
public class TenantDataSourceInterceptor implements SessionEventListener {

    private final Map<UUID, DataSource> dataSources = new ConcurrentHashMap<>();

    public void registerDataSource(UUID tenantId, DataSource dataSource) {
        dataSources.put(tenantId, dataSource);
    }

    @Override
    public void beforeStatementPrepare(StatementPreparedEvent event) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            // Logique pour changer de DataSource selon le tenant
        }
    }
}

// ============================================
// AOP ASPECT POUR L'AUDIT (Optionnel)
// ============================================

@Aspect
@Component
public class AuditAspect {

    @Around("@annotation(com.eyram.dev.church_project_spring.annotations.Auditable)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        UUID tenantId = TenantContext.getTenantId();
        Object result = joinPoint.proceed();
        
        // Logger l'action
        log.info("Action effectuée par tenant: {}", tenantId);
        
        return result;
    }
}

// Utilisation:
/*
@Auditable
@PostMapping
public void create(@RequestBody DemandeRequest req) {
    // Will be audited automatically
}
*/
