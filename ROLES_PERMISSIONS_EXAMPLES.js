// ============================================
// EXEMPLES D'UTILISATION - ROLES & PERMISSIONS
// ============================================

// ============================================
// 1. BACKEND EXAMPLES
// ============================================

// --- Example 1: Protéger un endpoint avec un rôle ---
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/demandes")
@RequiredArgsConstructor
public class DemandeController {

    private final DemandeService demandeService;

    // ✅ PUBLIC - Créer une demande (fidèle)
    @PostMapping
    public ResponseEntity<DemandeResponse> createDemande(
            @Valid @RequestBody DemandeRequest request) {
        return ResponseEntity.ok(demandeService.create(request));
    }

    // ✅ AUTHENTIFIÉ - Récupérer ses propres demandes
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DemandeResponse>> getMesDemandes() {
        return ResponseEntity.ok(demandeService.getMesDemandes());
    }

    // ✅ SECRETAIRE ou ADMIN - Mettre à jour une demande
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SECRETAIRE', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<DemandeResponse> updateDemande(
            @PathVariable UUID id,
            @Valid @RequestBody DemandeRequest request) {
        return ResponseEntity.ok(demandeService.update(id, request));
    }

    // ✅ SECRETAIRE ou ADMIN - Valider une demande
    @PostMapping("/{id}/validate")
    @PreAuthorize("hasAnyRole('SECRETAIRE', 'CURE', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<DemandeResponse> validateDemande(
            @PathVariable UUID id) {
        return ResponseEntity.ok(demandeService.validate(id));
    }

    // ✅ ADMIN ou SUPER_ADMIN - Supprimer une demande
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteDemande(
            @PathVariable UUID id) {
        demandeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

// --- Example 2: Vérifier les permissions manuellement ---
import com.eyram.dev.church_project_spring.security.RolePermissions;
import com.eyram.dev.church_project_spring.enums.UserRole;

@Service
@RequiredArgsConstructor
public class DemandeService {

    public void softDeleteDemande(UUID demandeId, UserRole userRole) {
        // Vérifier manuellement la permission
        if (!RolePermissions.canDelete(userRole)) {
            throw new ForbiddenException("Vous n'avez pas les permissions pour supprimer");
        }
        
        // Procéder...
    }

    public void validateDemande(UUID demandeId, UserRole userRole) {
        if (!RolePermissions.canValidate(userRole)) {
            throw new ForbiddenException("Seuls les curés et secrétaires peuvent valider");
        }
        // Procéder...
    }
}

// --- Example 3: Multi-Tenancy - Isoler par paroisse ---
import com.eyram.dev.church_project_spring.security.TenantContext;

@Service
@RequiredArgsConstructor
public class ParoisseService {

    private final DemandeRepository demandeRepository;

    public List<Demande> getDemandesForCurrentParoisse() {
        UUID paroisseId = TenantContext.getTenantId();
        
        if (paroisseId == null) {
            throw new BadRequestException("Aucune paroisse n'est définie");
        }
        
        // Retourner uniquement les demandes de cette paroisse
        return demandeRepository.findByParoisseId(paroisseId);
    }
}

// ============================================
// 2. FRONTEND EXAMPLES
// ============================================

// --- Example 1: Utiliser usePermissions() dans un composant ---
import { usePermissions } from '@/hooks/usePermissions';

function DemandePage() {
  const { can, isAdminUser, isSuperAdminUser, has } = usePermissions();

  return (
    <div>
      <h1>Mes Demandes</h1>

      {/* Bouttons basés sur les permissions */}
      {can.create && (
        <button className="bg-blue-500">
          Créer une demande
        </button>
      )}

      {can.edit && (
        <button className="bg-yellow-500">
          Éditer la demande
        </button>
      )}

      {can.delete && (
        <button className="bg-red-500">
          Supprimer la demande
        </button>
      )}

      {can.validate && (
        <button className="bg-green-500">
          Valider la demande
        </button>
      )}

      {/* Afficher le dashboard admin */}
      {isAdminUser && (
        <section className="mt-6">
          <h2>Dashboard Administration</h2>
          <p>Bienvenue administrateur!</p>
        </section>
      )}

      {/* Afficher les options système (super admin only) */}
      {isSuperAdminUser && (
        <section className="mt-6">
          <h2>Configuration Système</h2>
          <button>Gérer les locales</button>
          <button>Gérer les tarifs</button>
        </section>
      )}
    </div>
  );
}

// --- Example 2: Protéger une route avec RoleGuard ---
import { RoleGuard } from '@/app/guards/RoleGuard';
import { PermissionGuard } from '@/app/guards/PermissionGuard';

function AppRouter() {
  return (
    <Routes>
      {/* Public routes */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/public/demandes" element={<PublicDemandesPage />} />

      {/* Admin routes - protégées par RoleGuard */}
      <Route element={<RoleGuard allowedRoles={['ADMIN', 'SUPER_ADMIN']} />}>
        <Route path="/admin/dashboard" element={<AdminDashboard />} />
        <Route path="/admin/users" element={<UserManagement />} />
      </Route>

      {/* Secrétaire routes */}
      <Route element={<RoleGuard allowedRoles={['SECRETAIRE', 'ADMIN', 'SUPER_ADMIN']} />}>
        <Route path="/secretary/demandes" element={<SecretaireDemandes />} />
      </Route>

      {/* Routes nécessitant des permissions spécifiques */}
      <Route element={<PermissionGuard requiredPermissions={['delete']} />}>
        <Route path="/demandes/delete" element={<DeleteDemanedPage />} />
      </Route>

      {/* Page non autorisée */}
      <Route path="/unauthorized" element={<UnauthorizedPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}

// --- Example 3: Afficher les infos utilisateur et permissions ---
import { useAuth } from '@/hooks/useAuth';
import { usePermissions } from '@/hooks/usePermissions';
import { RoleBadge } from '@/components/ui/RoleComponents';
import { UserProfileCard } from '@/components/ui/UserProfileCard';

function ProfilePage() {
  const { user } = useAuth();
  const { can, allPermissions, isAdminUser } = usePermissions();

  return (
    <div className="container mx-auto p-6">
      <h1>Mon Profil</h1>

      {/* Card de profil */}
      <UserProfileCard />

      {/* Infos supplémentaires */}
      <div className="mt-6 bg-white rounded-lg shadow p-6">
        <h2>Informations de Sécurité</h2>
        
        <div className="mt-4">
          <h3>Rôle</h3>
          <RoleBadge role={user.role} />
        </div>

        <div className="mt-4">
          <h3>Permissions Activées</h3>
          <ul className="list-disc list-inside">
            {allPermissions.map(perm => (
              <li key={perm}>{perm}</li>
            ))}
          </ul>
        </div>

        <div className="mt-4">
          <h3>Accès Admin</h3>
          <p>{isAdminUser ? '✓ Activé' : '✗ Désactivé'}</p>
        </div>
      </div>
    </div>
  );
}

// --- Example 4: Formulaire de création utilisateur (Admin) ---
import { useState } from 'react';
import { RoleSelector } from '@/components/ui/RoleComponents';
import { usePermissions } from '@/hooks/usePermissions';

function CreateUserForm() {
  const { can } = usePermissions();
  const [formData, setFormData] = useState({
    nom: '',
    prenom: '',
    username: '',
    password: '',
    role: 'SECRETAIRE',
    isActive: true,
  });

  // Ne pas afficher si pas de permission créer/admin
  if (!can.create && !can.edit) {
    return <UnauthorizedPage />;
  }

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      const response = await apiClient('/users', {
        method: 'POST',
        body: JSON.stringify(formData),
      });

      alert('Utilisateur créé avec succès!');
      // Rediriger...
    } catch (error) {
      alert('Erreur lors de la création: ' + error.message);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="max-w-md mx-auto">
      <input
        type="text"
        value={formData.nom}
        onChange={(e) => setFormData({...formData, nom: e.target.value})}
        placeholder="Nom"
        required
      />

      <input
        type="text"
        value={formData.prenom}
        onChange={(e) => setFormData({...formData, prenom: e.target.value})}
        placeholder="Prénom"
        required
      />

      <input
        type="text"
        value={formData.username}
        onChange={(e) => setFormData({...formData, username: e.target.value})}
        placeholder="Username"
        required
      />

      <input
        type="password"
        value={formData.password}
        onChange={(e) => setFormData({...formData, password: e.target.value})}
        placeholder="Mot de passe"
        required
      />

      {/* RoleSelector */}
      <RoleSelector
        selectedRole={formData.role}
        onChange={(role) => setFormData({...formData, role})}
      />

      <label>
        <input
          type="checkbox"
          checked={formData.isActive}
          onChange={(e) => setFormData({...formData, isActive: e.target.checked})}
        />
        Actif
      </label>

      <button type="submit" className="bg-blue-500 text-white px-4 py-2">
        Créer l'utilisateur
      </button>
    </form>
  );
}

// --- Example 5: Appel API avec tenant ID (multi-tenancy) ---
import { apiClient } from '@/services/http/apiClient';
import { useTenant } from '@/hooks/useTenant';

async function getDemandesForCurrentTenant() {
  const { paroisseId } = useTenant();

  const response = await apiClient('/demandes', {
    method: 'GET',
    headers: {
      'X-Tenant-ID': paroisseId, // ← Ajouter le tenant ID
    },
  });

  return response;
}

// ============================================
// 3. CUSTOM HOOKS & UTILITIES
// ============================================

// --- Custom Hook: useCanAction ---
import { usePermissions } from '@/hooks/usePermissions';

export function useCanAction() {
  const { can, has, isAdminUser } = usePermissions();

  return {
    canCreateDemande: can.create,
    canEditDemande: can.edit,
    canDeleteDemande: can.delete,
    canValidateDemande: can.validate,
    canAccessAdmin: isAdminUser,
    canManageUsers: has('manage_users'),
  };
}

// Utilisation:
function MyComponent() {
  const { canEditDemande, canDeleteDemande } = useCanAction();

  return (
    <>
      {canEditDemande && <button>Éditer</button>}
      {canDeleteDemande && <button>Supprimer</button>}
    </>
  );
}

// ============================================
// 4. CREATING TEST DATA
// ============================================

// Script pour créer des utilisateurs test via l'API

const testUsers = [
  {
    nom: 'Admin',
    prenom: 'Test',
    username: 'admin_test',
    password: 'AdminPass123!',
    role: 'ADMIN',
    isGlobal: false,
    isActive: true,
  },
  {
    nom: 'Secrétaire',
    prenom: 'Test',
    username: 'secretary_test',
    password: 'SecPass123!',
    role: 'SECRETAIRE',
    isGlobal: false,
    isActive: true,
  },
  {
    nom: 'Curé',
    prenom: 'Test',
    username: 'priest_test',
    password: 'PriestPass123!',
    role: 'CURE',
    isGlobal: false,
    isActive: true,
  },
  {
    nom: 'Cure',
    prenom: 'Test',
    username: 'cure_test',
    password: 'CurePass123!',
    role: 'CURE',
    isGlobal: false,
    isActive: true,
  },
];

async function createTestUsers() {
  for (const user of testUsers) {
    try {
      const response = await fetch('http://localhost:8081/users', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${adminToken}`,
        },
        body: JSON.stringify(user),
      });
      console.log(`Créé: ${user.username}`);
    } catch (error) {
      console.error(`Erreur pour ${user.username}:`, error);
    }
  }
}
