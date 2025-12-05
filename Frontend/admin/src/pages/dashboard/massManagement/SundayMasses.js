import React from "react";

const SundayMasses = () => {
  // Exemple de structure de données (sera remplacée par les données du backend)
  const sundayMasses = [
    {
      date: "2025-03-02",
      masses: [
        {
          hour: "07:00",
          intentions: [
            {
              id: "INT001",
              requester: "Kossi David",
              intention: "Pour la paix dans la famille",
              offering: 3000,
              status: "Validée",
            },
            {
              id: "INT002",
              requester: "Afi Mawuto",
              intention: "Action de grâce",
              offering: 3000,
              status: "Validée",
            },
          ],
        },
        {
          hour: "09:00",
          intentions: [
            {
              id: "INT003",
              requester: "John Doe",
              intention: "Pour le repos de l’âme de Kodjo",
              offering: 3000,
              status: "Validée",
            },
          ],
        },
      ],
    },
    {
      date: "2025-03-09",
      masses: [
        {
          hour: "07:00",
          intentions: [
            {
              id: "INT004",
              requester: "Paul K.",
              intention: "Protection divine",
              offering: 3000,
              status: "Validée",
            },
          ],
        },
      ],
    },
  ];

  return (
    <div className="p-6">
      <h1 className="text-2xl font-semibold mb-4">Messes Dominicales</h1>

      {sundayMasses.map((sunday) => (
        <div
          key={sunday.date}
          className="mb-8 bg-white shadow-sm rounded-lg border border-gray-100"
        >
          {/* Date du dimanche */}
          <div className="px-5 py-4 border-b bg-gray-50">
            <h2 className="text-lg font-semibold">
              Dimanche {new Date(sunday.date).toLocaleDateString("fr-FR")}
            </h2>
          </div>

          <div className="p-5 space-y-8">
            {sunday.masses.map((mass, index) => (
              <div key={index} className="border rounded-lg">
                {/* Horaire */}
                <div className="px-4 py-2 font-medium bg-gray-100 border-b">
                  Messe de {mass.hour}
                </div>

                {/* Tableau des intentions */}
                <div className="overflow-x-auto">
                  <table className="min-w-full text-sm">
                    <thead>
                      <tr className="bg-gray-50 border-b">
                        <th className="px-4 py-2 text-left">ID</th>
                        <th className="px-4 py-2 text-left">Demandeur</th>
                        <th className="px-4 py-2 text-left">Intention</th>
                        <th className="px-4 py-2 text-left">Offrande</th>
                        <th className="px-4 py-2 text-left">Statut</th>
                      </tr>
                    </thead>
                    <tbody>
                      {mass.intentions.map((intent) => (
                        <tr key={intent.id} className="border-b">
                          <td className="px-4 py-2">{intent.id}</td>
                          <td className="px-4 py-2">{intent.requester}</td>
                          <td className="px-4 py-2">{intent.intention}</td>
                          <td className="px-4 py-2">{intent.offering} FCFA</td>
                          <td className="px-4 py-2">
                            <span
                              className={`px-2 py-1 rounded text-xs ${
                                intent.status === "Validée"
                                  ? "bg-green-100 text-green-700"
                                  : "bg-yellow-100 text-yellow-700"
                              }`}
                            >
                              {intent.status}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
};

export default SundayMasses;
