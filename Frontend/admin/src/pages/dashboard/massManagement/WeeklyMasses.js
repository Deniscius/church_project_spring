import React, { useEffect, useState } from "react";

const WeeklyMasses = () => {
    const [loading, setLoading] = useState(true);
    const [masses, setMasses] = useState([]);
    const [error, setError] = useState(null);

    // Simulation API (à remplacer par ton fetch réel)
    useEffect(() => {
        const fetchMasses = async () => {
            try {
                // Exemple de structure venant d'une API
                const data = [
                    {
                        id: 1,
                        date: "2025-02-10",
                        time: "06:00",
                        intention: "Pour le repos de l’âme de Koffi Mensah",
                        type: "Intention pour défunt",
                        requesterName: "Afi Lawson",
                        status: "paid",
                    },
                    {
                        id: 2,
                        date: "2025-02-11",
                        time: "18:00",
                        intention: "Action de grâce pour la famille Kodjo",
                        type: "Action de grâce",
                        requesterName: "Jean Kodjo",
                        status: "unpaid",
                    },
                    {
                        id: 3,
                        date: "2025-02-12",
                        time: "07:30",
                        intention: "Protection et santé pour Akofa",
                        type: "Protection",
                        requesterName: "Akofa Dovi",
                        status: "paid",
                    }
                ];

                setMasses(data);
                setLoading(false);
            } catch (err) {
                setError("Impossible de charger les messes programmées.");
                setLoading(false);
            }
        };

        fetchMasses();
    }, []);

    if (loading) {
        return (
            <div className="w-full flex justify-center items-center h-48">
                <p className="text-gray-600 font-medium animate-pulse">Chargement des messes…</p>
            </div>
        );
    }

    if (error) {
        return (
            <div className="w-full text-center text-red-500 font-semibold">
                {error}
            </div>
        );
    }

    return (
        <div className="w-full p-6">
            <h1 className="text-2xl font-bold text-gray-800 mb-6">
                Messes Hebdomadaires — Paroisse
            </h1>

            <div className="bg-white shadow rounded-lg overflow-hidden border border-gray-100">
                <table className="min-w-full text-left">
                    <thead className="bg-gray-50 border-b border-gray-200">
                        <tr>
                            <th className="px-6 py-3 text-sm font-semibold text-gray-600">Date</th>
                            <th className="px-6 py-3 text-sm font-semibold text-gray-600">Heure</th>
                            <th className="px-6 py-3 text-sm font-semibold text-gray-600">Intention</th>
                            <th className="px-6 py-3 text-sm font-semibold text-gray-600">Type</th>
                            <th className="px-6 py-3 text-sm font-semibold text-gray-600">Fidèle</th>
                            <th className="px-6 py-3 text-sm font-semibold text-gray-600">Statut</th>
                            <th className="px-6 py-3 text-sm font-semibold text-gray-600"></th>
                        </tr>
                    </thead>

                    <tbody className="divide-y divide-gray-200">
                        {masses.map((mass) => (
                            <tr key={mass.id} className="hover:bg-gray-50">
                                <td className="px-6 py-4 text-gray-800 font-medium">
                                    {new Date(mass.date).toLocaleDateString("fr-FR", {
                                        weekday: "long",
                                        year: "numeric",
                                        month: "long",
                                        day: "numeric"
                                    })}
                                </td>

                                <td className="px-6 py-4 text-gray-700 font-medium">{mass.time}</td>

                                <td className="px-6 py-4 text-gray-700">{mass.intention}</td>

                                <td className="px-6 py-4 text-gray-700">{mass.type}</td>

                                <td className="px-6 py-4 text-gray-700">{mass.requesterName}</td>

                                <td className="px-6 py-4">
                                    {mass.status === "paid" ? (
                                        <span className="px-3 py-1 rounded-full text-sm font-semibold bg-green-100 text-green-700">
                                            Payée
                                        </span>
                                    ) : (
                                        <span className="px-3 py-1 rounded-full text-sm font-semibold bg-red-100 text-red-700">
                                            Non payée
                                        </span>
                                    )}
                                </td>

                                <td className="px-6 py-4 text-right">
                                    <button className="px-4 py-2 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition">
                                        Voir détails
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>

                {masses.length === 0 && (
                    <div className="text-center py-10 text-gray-500 font-medium">
                        Aucune messe programmée pour cette semaine.
                    </div>
                )}
            </div>
        </div>
    );
};

export default WeeklyMasses;
