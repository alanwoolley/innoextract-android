/*
 * Copyright (C) 2011-2015 Daniel Scharrer
 *
 * This software is provided 'as-is', without any express or implied
 * warranty.  In no event will the author(s) be held liable for any damages
 * arising from the use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 *
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */

#include "release.hpp"

/*
 * \file
 *
 * This file is automatically processed by cmake if the version or commit id changes.
 * For the exact syntax see the documentation of the configure_file() cmake command.
 * For available variables see cmake/VersionString.cmake.
 */

#if 2 != 2
#error "Configure error - the VERSION file should specify exactly two lines!"
#endif

#if 15 < 3
#error "Configure error - the LICENSE file should specify at least three lines!"
#endif

const char innoextract_name[] = "innoextract";

const char innoextract_version[] = "1.6";

const char innosetup_versions[] = "Inno Setup 1.2.10 to 5.5.8";

const char innoextract_bugs[] = "http://innoextract.constexpr.org/issues";

const char innoextract_copyright[] = "Copyright (C) 2011-2016 Daniel Scharrer <daniel@constexpr.org>";

const char innoextract_license[] = "This software is provided 'as-is', without any express or implied\nwarranty.  In no event will the author(s) be held liable for any damages\narising from the use of this software.\n\nPermission is granted to anyone to use this software for any purpose,\nincluding commercial applications, and to alter it and redistribute it\nfreely, subject to the following restrictions:\n\n1. The origin of this software must not be misrepresented; you must not\n   claim that you wrote the original software. If you use this software\n   in a product, an acknowledgment in the product documentation would be\n   appreciated but is not required.\n2. Altered source versions must be plainly marked as such, and must not be\n   misrepresented as being the original software.\n3. This notice may not be removed or altered from any source distribution.\n";
