import { useCallback, useEffect, useRef, useState } from 'react';
import { flushSync } from 'react-dom';
import { useLocation } from 'react-router-dom';

// Tailwind's `lg` breakpoint: at and above it the sidebar is always visible and the drawer is unused
const DESKTOP_QUERY = '(min-width: 1024px)';

/**
 * Open/close state for a navigation sidebar that becomes an off-canvas drawer below the `lg` breakpoint.
 *
 * The drawer closes on any navigation (links, back/forward), on Escape, and when the viewport grows to
 * desktop width. Opening moves focus to the drawer's close button; closing it with Escape, the close
 * button or the backdrop returns focus to the menu button. Page scrolling is locked while it is open.
 * Attach `menuButtonRef` to the menu toggle and `closeButtonRef` to the close button inside the drawer.
 */
export function useMobileDrawer() {
  const { pathname } = useLocation();
  const [open, setOpen] = useState(false);
  const menuButtonRef = useRef(null);
  const closeButtonRef = useRef(null);

  const openDrawer = useCallback(() => setOpen(true), []);

  const closeDrawer = useCallback(() => {
    // Commit first: the page behind the drawer is inert while it is open, so the menu button
    // can only take focus once the drawer has actually closed.
    flushSync(() => setOpen(false));
    menuButtonRef.current?.focus();
  }, []);

  useEffect(() => {
    setOpen(false);
  }, [pathname]);

  useEffect(() => {
    if (!open) return undefined;

    closeButtonRef.current?.focus();

    const onKeyDown = (event) => {
      if (event.key === 'Escape') closeDrawer();
    };
    const desktop = window.matchMedia(DESKTOP_QUERY);
    const onViewportChange = (event) => {
      if (event.matches) setOpen(false);
    };
    const previousOverflow = document.body.style.overflow;

    document.addEventListener('keydown', onKeyDown);
    desktop.addEventListener('change', onViewportChange);
    document.body.style.overflow = 'hidden';

    return () => {
      document.removeEventListener('keydown', onKeyDown);
      desktop.removeEventListener('change', onViewportChange);
      document.body.style.overflow = previousOverflow;
    };
  }, [open, closeDrawer]);

  return { open, openDrawer, closeDrawer, menuButtonRef, closeButtonRef };
}

/** Backdrop behind the open drawer; above the floating language selector (z-50), below the drawer. */
export const DRAWER_BACKDROP_CLASSES = 'fixed inset-0 z-[55] bg-black/50 backdrop-blur-xs lg:hidden';

/**
 * Classes for the sidebar element: a fixed off-canvas drawer below `lg`, a sticky full-height column at `lg`+.
 * While closed on small screens it is also `invisible`, so its links are not reachable with Tab or a screen
 * reader. Visibility is transitioned only when closing, so the slide-out stays visible to the end while an
 * opening drawer is focusable immediately. At `lg`+ it has no z-index, so page modals (z-50) cover it.
 */
export function drawerPanelClasses(open) {
  return [
    'fixed inset-y-0 left-0 z-[60] overflow-y-auto duration-300 ease-in-out',
    'lg:sticky lg:top-0 lg:bottom-auto lg:z-auto lg:h-screen lg:shrink-0 lg:visible lg:translate-x-0',
    open ? 'visible translate-x-0 transition-transform' : 'invisible -translate-x-full transition-[translate,visibility]',
  ].join(' ');
}
