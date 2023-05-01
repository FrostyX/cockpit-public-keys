Name: cockpit-public-keys
Version: 1.0
Release: 1%{?dist}
Summary: Search GitHub users and authorize their SSH keys with just a few clicks
License: GPL-2.0-or-later
URL: https://github.com/frostyx/cockpit-public-keys
Source0: %{version}/%{name}-%{version}.tar.gz

BuildArch: noarch
ExclusiveArch: %{nodejs_arches} noarch

BuildRequires: nodejs
BuildRequires: nodejs-devel
BuildRequires: make
BuildRequires: libappstream-glib
BuildRequires: gettext
%if 0%{?rhel} && 0%{?rhel} <= 8
BuildRequires: libappstream-glib-devel
%endif

Requires: cockpit-bridge


%description
Cockpit Public Keys Module


%prep
%setup -q


%build
make css
make build


%install
mkdir -p %{buildroot}/%{_datadir}/%{name}/public/css
cp -a public/css/style.css %{buildroot}/%{_datadir}/%{name}/public/css
cp -ar public/js %{buildroot}/%{_datadir}/%{name}
cp -ar index.html %{buildroot}/%{_datadir}/%{name}


%check
appstream-util validate-relax --nonet %{buildroot}/%{_datadir}/metainfo/*


%files
%doc README.md
%license LICENSE dist/index.js.LEGAL.txt dist/index.css.LEGAL.txt
%{_datadir}/cockpit/*
%{_datadir}/metainfo/*


%changelog
* Mon May 01 2023 Jakub Kadlcik <frostyx@email.cz> 1.0-1
- Initial package
