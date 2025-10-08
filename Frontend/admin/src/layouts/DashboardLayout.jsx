import React, { useState } from 'react';
import { Link, NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { MdClose, MdMenu } from 'react-icons/md';


const DashboardLayout = () => {

  const [isSidebarOpen, setIsSidebarOpen] = useState(true); // burger menu

  const toggleSidebar = () => setIsSidebarOpen(!isSidebarOpen);

  const activeSidenavLinkCss = 'bg-terracotta flex justify-start items-center gap-[10px] p-4 cursor-pointer text-white text-[14px] font-bold rounded w-[80%]';
  const sidenavLinkCss = 'flex justify-start items-center text-white gap-[10px] p-4 cursor-pointer text-[14px] rounded w-[80%] hover:bg-grey';

  const sidebarList = [
    {
      label: 'Dashboard',
      link: '/dashboard/',
    },

  ];

  const [isOpen, setIsOpen] = useState(false);
  const navigate = useNavigate();

  const toggleProfileBtn = () => {
    setIsOpen(!isOpen);
  };

  const [modalOpen, setModelOpen] = useState(false);

  const handleClose = () => {
    setModelOpen(false);
  }

  const location = useLocation();

  const isActiveLink = (path) => {
    if (path === '/dashboard/users') {
      return location.pathname.startsWith('/dashboard/users');
    }

    return location.pathname === path;
  };

  return (
    <div className="h-screen w-screen flex bg-white">
      {/* SIDEBAR */}
      <section
        className={`bg-white h-full shadow-2xl border-r min-w-[250px] max-w-[80%] flex flex-col z-50
      fixed  transition-transform duration-300
      ${isSidebarOpen ? "translate-x-0 xl:relative" : "-translate-x-full"}`}
      >
        <Link to={"/dashboard/"}>
          LOGO
        </Link>

        <div className="w-full h-full flex overflow-y-auto scrollBar">
          <div className="h-full w-full flex flex-col gap-5 items-center select-none font-agrandir">
            {sidebarList.map((list, idx) => (
              <NavLink
                key={idx}
                to={list.link}
                className={
                  isActiveLink(list.link)
                    ? activeSidenavLinkCss
                    : sidenavLinkCss
                }
                end
                onClick={() => setIsSidebarOpen(false)}
              >
                {list.label}
              </NavLink>
            ))}

            <div
              className="absolute bottom-[50px] flex justify-start items-center border-2 border-black text-black gap-[10px] p-4 cursor-pointer text-[14px] rounded w-[80%] hover:bg-grey"
            >
              Déconnexion
            </div>
          </div>
        </div>
      </section>

      {/* MAIN CONTENT */}
      <section className="flex flex-col w-full overflow-y-scroll bg-white">
        {/* TOPBAR */}
        <div className="flex justify-between items-center p-5 w-full">

          <p className="text-black">LOGO</p>

          {/* Burger visible partout */}
          <button
            className="text-black text-3xl"
            onClick={toggleSidebar}
          >
            {isSidebarOpen ? <MdClose /> : <MdMenu />}
          </button>

        </div>

        <div className="px-5 py-10">
          <Outlet />
        </div>
      </section>
    </div>
  )
}

export default DashboardLayout