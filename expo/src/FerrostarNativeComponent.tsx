import { requireNativeViewManager } from "expo-modules-core";
import React, {
  forwardRef,
  useContext,
  useImperativeHandle,
  useLayoutEffect,
  useRef,
} from "react";
import { StyleProp, ViewStyle } from "react-native";

import {
  ExpoFerrostarModule,
  NativeViewProps,
  NavigationControllerConfig,
  FerrostarViewProps,
  Route,
  UserLocation,
  Waypoint,
  NavigationStateChangeEvent,
} from "./ExpoFerrostar.types";
import { FerrostarContext } from "./FerrostarProvider";

const NativeView: React.ComponentType<
  NativeViewProps & {
    ref: React.RefObject<ExpoFerrostarModule>;
    style: StyleProp<ViewStyle>;
    onNavigationStateChange?: (event: NavigationStateChangeEvent) => void;
  }
> = requireNativeViewManager("ExpoFerrostar");

export const FerrostarNativeComponent = forwardRef<
  ExpoFerrostarModule,
  FerrostarViewProps & {
    style: StyleProp<ViewStyle>;
    onNavigationStateChange?: (event: NavigationStateChangeEvent) => void;
  }
>((props, ref) => {
  const innerRef = useRef<ExpoFerrostarModule>(null);

  const { refs } = useContext(FerrostarContext);

  useLayoutEffect(() => {
    if (!innerRef.current) return;

    if (props.id === "current") {
      console.error("'current' cannot be used as ferrostar id");
    }

    if (props.id && refs[props.id]) {
      console.error(
        `Multiple ferrostar instances withe the same id: ${props.id}`
      );
    }

    if (props.id) {
      refs[props.id] = innerRef.current;
    }

    refs["current"] = innerRef.current;

    return () => {
      if (props.id) {
        refs[props.id] = undefined;
      }
      if (refs["current"] === innerRef.current) {
        refs["current"] = undefined;
      }
    };
  }, [props.id, refs, innerRef.current]);

  useImperativeHandle(ref, () => ({
    createRouteFromOsrm: async (route: string, waypoints: string) => {
      if (!innerRef.current) return null;
      return await innerRef.current.createRouteFromOsrm(route, waypoints);
    },
    startNavigation: (route: Route, options?: NavigationControllerConfig) => {
      innerRef.current?.startNavigation(route, options);
    },
    stopNavigation: (stopLocationUpdates?: boolean) => {
      innerRef.current?.stopNavigation(stopLocationUpdates);
    },
    replaceRoute: (route: Route, options?: NavigationControllerConfig) => {
      innerRef.current?.replaceRoute(route, options);
    },
    advanceToNextStep: () => {
      innerRef.current?.advanceToNextStep();
    },
    getRoutes: async (initialLocation: UserLocation, waypoints: Waypoint[]) => {
      if (!innerRef.current) return [];
      return await innerRef.current.getRoutes(initialLocation, waypoints);
    },
  }));

  return (
    <NativeView
      ref={innerRef}
      style={props.style}
      onNavigationStateChange={(e) => {
        props.onNavigationStateChange?.(e);
      }}
      navigationOptions={{
        ...props,
      }}
      coreOptions={{
        ...props,
      }}
    />
  );
});
