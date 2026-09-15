import type { MapPadding } from './_types';

export type NavigationViewLayout = 'dynamic' | 'landscape' | 'portrait';

export const resolveNavigationViewLayout = (
  layout: NavigationViewLayout,
  width: number,
  height: number
): Exclude<NavigationViewLayout, 'dynamic'> => {
  if (layout !== 'dynamic') return layout;

  return width > height ? 'landscape' : 'portrait';
};

export const navigationViewCameraPadding = (
  layout: NavigationViewLayout,
  width: number,
  height: number,
  isRightToLeft: boolean,
  padding?: MapPadding
): MapPadding | undefined => {
  const resolvedLayout = resolveNavigationViewLayout(layout, width, height);
  if (resolvedLayout === 'portrait') return padding;

  const leadingPadding = width / 2;

  return {
    top: padding?.top ?? 0,
    right: (padding?.right ?? 0) + (isRightToLeft ? leadingPadding : 0),
    bottom: padding?.bottom ?? 0,
    left: (padding?.left ?? 0) + (isRightToLeft ? 0 : leadingPadding),
  };
};
